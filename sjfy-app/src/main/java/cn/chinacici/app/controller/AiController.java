package cn.chinacici.app.controller;

import cn.chinacici.app.dto.AiChatRequestDto;
import cn.chinacici.core.ResponseData;
import cn.chinacici.core.ResultCode;
import cn.chinacici.exception.ServiceException;
import cn.chinacici.service.ai.AiChatService;
import cn.chinacici.service.ai.dto.AiChatResult;
import cn.chinacici.service.auth.AuthService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 八字 AI 助手接口。
 *
 * <p>接口必须先校验登录态，再调用 DeepSeek。这样前端不会暴露模型密钥。</p>
 */
@RestController
@RequestMapping("/ai")
public class AiController {
    private final AuthService authService;
    private final AiChatService aiChatService;

    public AiController(AuthService authService, AiChatService aiChatService) {
        this.authService = authService;
        this.aiChatService = aiChatService;
    }

    /**
     * 带完整八字上下文发起 AI 问答。
     */
    @PostMapping("/chat")
    public ResponseData<AiChatResult> chat(@RequestHeader(value = "Authorization", required = false) String authorization,
                                           @RequestBody AiChatRequestDto requestDto) {
        if (requestDto == null) {
            throw new ServiceException(ResultCode.PARAMETER_ERROR, "请输入想问 AI 的问题");
        }
        authService.requireSession(authorization);
        AiChatResult result = aiChatService.chat(
                requestDto.getQuestion(),
                requestDto.getBaziContext(),
                requestDto.getHistory(),
                requestDto.getModel(),
                requestDto.getThinkingEnabled(),
                requestDto.getReasoningEffort()
        );
        return ResponseData.success(result);
    }
}
