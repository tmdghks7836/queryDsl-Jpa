package rc.meeting.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.support.SpringBootServletInitializer;

@SpringBootApplication
public class RcMeetingServer extends SpringBootServletInitializer {

    public static void main(String... args) {
        SpringApplication app = new SpringApplication(RcMeetingServer.class);
        app.run(args);
    }

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
        return builder.sources(RcMeetingServer.class);
    }

}
