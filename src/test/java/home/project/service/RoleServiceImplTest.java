package home.project.service;

import home.project.domain.Role;
import home.project.repository.RoleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
public class RoleServiceImplTest {

    @MockBean
    private RoleRepository roleRepository;

    @Autowired
    private RoleServiceImpl roleService;

    private Role role;

    @BeforeEach
    void setUp(){
        role = new Role();
        role.setId(1L);
        role.setRole("user");
    }

    @Nested
    class FindByIdTests {
        @Test
        void findById_ExistingRole_ReturnsRole() {
            when(roleRepository.findById(1L)).thenReturn(Optional.of(role));
            Optional<Role> findRole = roleService.findById(1L);

            assertTrue(findRole.isPresent());
            assertEquals(role, findRole.get());
        }

        @Test
        void findById_NonExistingRole_ThrowsIllegalArgumentException() {
            when(roleRepository.findById(1L)).thenReturn(Optional.empty());

            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> roleService.findById(1L));
            assertEquals("1(으)로 등록된 회원이 없습니다.", exception.getMessage());
        }
    }

    @Nested
    class UpdateTests {
        @Test
        void update_ExistingRole_ReturnsUpdatedRole() {
            when(roleRepository.findById(1L)).thenReturn(Optional.of(role));

            Role updateRole = new Role();
            updateRole.setId(role.getId());
            updateRole.setRole("user");

            Optional<Role> resultRole = roleService.update(updateRole);
            assertTrue(resultRole.isPresent());
            assertEquals("user", resultRole.get().getRole());
        }
        @Test
        void update_NonExistingRole_ThrowsIllegalArgumentException() {
            Role updateRole = new Role();
            updateRole.setId(999L);

            when(roleRepository.findById(999L)).thenReturn(Optional.empty());
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> roleService.update(updateRole));

            assertEquals("999(으)로 등록된 회원이 없습니다.", exception.getMessage());
        }
    }


    @Nested
    class JoinTests {
        @Test
        void join_NonExistingRole_SavesRole() {
            when(roleRepository.save(role)).thenReturn(role);

            roleService.join(role);

            verify(roleRepository).save(role);

            assertEquals("user", role.getRole());
            assertEquals(1L, role.getId());
        }
    }
}
