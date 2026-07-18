package recruitment.dev.authservice.confi;

import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;


@Configuration
public class FeignSecurityConfig {


    @Bean
    public RequestInterceptor requestInterceptor(){

        return template -> {

            var authentication =
                    SecurityContextHolder
                            .getContext()
                            .getAuthentication();


            if(authentication instanceof JwtAuthenticationToken jwt){

                template.header(
                        "Authorization",
                        "Bearer " + jwt.getToken().getTokenValue()
                );
            }
        };
    }
}