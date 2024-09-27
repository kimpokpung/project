package home.project.service;

import home.project.domain.Member;
import home.project.domain.Role;
import home.project.domain.RoleType;
import home.project.dto.requestDTO.CreateMemberRequestDTO;
import home.project.dto.requestDTO.UpdateMemberRequestDTO;
import home.project.dto.requestDTO.VerifyUserRequestDTO;
import home.project.dto.responseDTO.MemberResponse;
import home.project.dto.responseDTO.TokenResponse;
import home.project.exceptions.exception.IdNotFoundException;
import home.project.exceptions.exception.NoChangeException;
import home.project.repository.MemberRepository;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.Optional;

@RequiredArgsConstructor
@Service
@Transactional(readOnly = true)
public class MemberServiceImpl implements MemberService {
    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final RoleService roleService;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public Member convertToEntity(CreateMemberRequestDTO memberDTOWithoutId) {
        Member member = new Member();
        member.setEmail(memberDTOWithoutId.getEmail());
        member.setPassword(passwordEncoder.encode(memberDTOWithoutId.getPassword()));
        member.setName(memberDTOWithoutId.getName());
        member.setPhone(memberDTOWithoutId.getPhone());
        return member;
    }

    @Override
    @Transactional
    public TokenResponse join(CreateMemberRequestDTO createMemberRequestDTO) {

        if (!createMemberRequestDTO.getPassword().equals(createMemberRequestDTO.getPasswordConfirm())) {
            throw new IllegalStateException("비밀번호와 비밀번호 확인이 일치하지 않습니다.");
        }

        boolean emailExists = memberRepository.existsByEmail(createMemberRequestDTO.getEmail());
        boolean phoneExists = memberRepository.existsByPhone(createMemberRequestDTO.getPhone());
        if (emailExists && phoneExists) {
            throw new DataIntegrityViolationException("이미 사용 중인 이메일과 전화번호입니다.");
        } else if (emailExists) {
            throw new DataIntegrityViolationException("이미 사용 중인 이메일입니다.");
        } else if (phoneExists) {
            throw new DataIntegrityViolationException("이미 사용 중인 전화번호입니다.");
        }

        Member member = convertToEntity(createMemberRequestDTO);
        memberRepository.save(member);
        Member memberForAddRole = findByEmail(member.getEmail());
        Long id = memberForAddRole.getId();

        Role role = new Role();
        role.setId(id);
        roleService.join(role);

        RoleType savedRole = roleService.findById(id).getRole();

        Authentication authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(createMemberRequestDTO.getEmail(), createMemberRequestDTO.getPassword()));
        TokenResponse TokenResponse = jwtTokenProvider.generateToken(authentication);

        TokenResponse.setRole(savedRole);

        return TokenResponse;
    }

    @Override
    public MemberResponse memberInfo() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        Long memberId = findByEmail(email).getId();
        Member member = findById(memberId);
        RoleType role = roleService.findById(memberId).getRole();
        MemberResponse memberResponse = new MemberResponse(member.getId(), member.getEmail(), member.getName(), member.getPhone(), role);
        return memberResponse;
    }

    @Override
    public Member findById(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new IdNotFoundException(memberId + "(으)로 등록된 회원이 없습니다."));
    }

    @Override
    public Member findByEmail(String email) {
        return memberRepository.findByEmail(email)
                .orElseThrow(() -> new IdNotFoundException(email + "(으)로 등록된 회원이 없습니다."));
    }

    @Override
    public Page<Member> findAll(Pageable pageable) {
        return memberRepository.findAll(pageable);
    }

    @Override
    public Page<MemberResponse> convertToMemberDTOWithoutPW(Page<Member> pagedMember) {
        Page<MemberResponse> pagedMemberResponse = pagedMember.map(member -> {
            Long roleId = member.getId();
            RoleType roleName = RoleType.valueOf("No Role");
            if (roleId != null) {
                Role role = roleService.findById(roleId);
                roleName = role.getRole();
            }
            return new MemberResponse(member.getId(), member.getEmail(), member.getName(), member.getPhone(), roleName);
        });
        return pagedMemberResponse;
    }

    @Override
    public Page<Member> findMembers(String name, String email, String phone, String role, String content, Pageable pageable) {
        return memberRepository.findMembers(name, email, phone, role, content, pageable);
    }

    @Override
    public String stringBuilder(String name, String email, String phone, String role, String content, Page<MemberResponse> pagedMemberDTOWithoutPw) {
        StringBuilder searchCriteria = new StringBuilder();
        if (name != null) searchCriteria.append(name).append(", ");
        if (email != null) searchCriteria.append(email).append(", ");
        if (phone != null) searchCriteria.append(phone).append(", ");
        if (role != null) searchCriteria.append(role).append(", ");
        if (content != null) searchCriteria.append(content).append(", ");

        String successMessage;
        if (!searchCriteria.isEmpty()) {
            searchCriteria.setLength(searchCriteria.length() - 2);
            successMessage = "검색 키워드 : " + searchCriteria;
        } else {
            successMessage = "전체 회원입니다.";
        }
        long totalCount = pagedMemberDTOWithoutPw.getTotalElements();
        if (totalCount == 0) {
            successMessage = "검색 결과가 없습니다. 검색 키워드 : " + searchCriteria;
        }

        return successMessage;
    }

    @Override
    public String verifyUser(String email, VerifyUserRequestDTO password) {

        Long id = findByEmail(email).getId();
        if (!passwordEncoder.matches(password.getPassword(), findByEmail(email).getPassword())) {
            throw new BadCredentialsException("비밀번호가 일치하지 않습니다.");
        }

        return jwtTokenProvider.generateVerificationToken(email, id);
    }

    @Override
    @Transactional
    public MemberResponse update(UpdateMemberRequestDTO updateMemberRequestDTO, String verificationToken) {
        String email = jwtTokenProvider.getEmailFromToken(verificationToken);
        if (email == null) {
            throw new JwtException("유효하지 않은 본인인증 토큰입니다. 본인인증을 다시 진행해주세요.");
        }
        if (!updateMemberRequestDTO.getPassword().equals(updateMemberRequestDTO.getPasswordConfirm())) {
            throw new IllegalStateException("비밀번호와 비밀번호 확인이 일치하지 않습니다.");
        }

        Long id = Long.parseLong(jwtTokenProvider.getIdFromVerificationToken(verificationToken));
        Member existingMember = findById(id);

        boolean isModified = false;
        boolean isEmailDuplicate = false;
        boolean isPhoneDuplicate = false;

        if (updateMemberRequestDTO.getName() != null && !updateMemberRequestDTO.getName().equals(existingMember.getName())) {
            existingMember.setName(updateMemberRequestDTO.getName());
            isModified = true;
        }

        if (updateMemberRequestDTO.getEmail() != null && !updateMemberRequestDTO.getEmail().equals(existingMember.getEmail())) {
            if (memberRepository.existsByEmail(updateMemberRequestDTO.getEmail())) {
                isEmailDuplicate = true;
            } else {
                existingMember.setEmail(updateMemberRequestDTO.getEmail());
                isModified = true;
            }
        }

        if (updateMemberRequestDTO.getPhone() != null && !updateMemberRequestDTO.getPhone().equals(existingMember.getPhone())) {
            if (memberRepository.existsByPhone(updateMemberRequestDTO.getPhone())) {
                isPhoneDuplicate = true;
            } else {
                existingMember.setPhone(updateMemberRequestDTO.getPhone());
                isModified = true;
            }
        }

        if (updateMemberRequestDTO.getPassword() != null && !passwordEncoder.matches(updateMemberRequestDTO.getPassword(), existingMember.getPassword())) {
            existingMember.setPassword(passwordEncoder.encode(updateMemberRequestDTO.getPassword()));
            isModified = true;
        }

        if (isEmailDuplicate && isPhoneDuplicate) {
            throw new DataIntegrityViolationException("이미 사용 중인 이메일과 전화번호입니다.");
        } else if (isEmailDuplicate) {
            throw new DataIntegrityViolationException("이미 사용 중인 이메일입니다.");
        } else if (isPhoneDuplicate) {
            throw new DataIntegrityViolationException("이미 사용 중인 전화번호입니다.");
        }

        if (!isModified) {
            throw new NoChangeException("변경된 회원 정보가 없습니다.");
        }

        Member updatedMember = memberRepository.save(existingMember);
        return new MemberResponse(updatedMember.getId(), updatedMember.getEmail(), updatedMember.getName(), updatedMember.getPhone(), updatedMember.getRole().getRole());
    }

    @Override
    @Transactional
    public void deleteById(Long memberId) {
        findById(memberId);
        memberRepository.deleteById(memberId);
    }

}
