package home.project.domain;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class UserDetails {
    @NotEmpty(message = "이메일은 필수 입력값입니다.")
    @Email(message = "이메일 형식이 올바르지 않습니다")
    private String email;

//비밀번호 형식은 나중에 확정해야 함
    @NotEmpty(message = "비밀번호는 필수 입력값입니다.")
    private String password;
//    public UsernamePasswordAuthenticationToken toAuthentication ( ) { return new UsernamePasswordAuthenticationToken(email, password);}
}
