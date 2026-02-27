import org.springframework.ai.chat.client.advisor.api.AdvisedRequest;
import org.springframework.ai.chat.client.advisor.ToolCallAdvisor;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
public class TestOrder {
    public static void main(String[] args) {
        System.out.println(MessageChatMemoryAdvisor.builder(null).build().getOrder());
        System.out.println(ToolCallAdvisor.builder().build().getOrder());
    }
}

