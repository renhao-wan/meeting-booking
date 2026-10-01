package io.github.renhaowan.booking.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import io.github.renhaowan.booking.common.BusinessException;
import io.github.renhaowan.booking.dao.UserMapper;
import io.github.renhaowan.booking.domain.User;
import io.github.renhaowan.booking.dto.LoginResponse;
import io.github.renhaowan.booking.service.impl.AuthServiceImpl;
import io.github.renhaowan.booking.service.impl.JwtServiceImpl;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * 登录服务单元测试（mock UserMapper，不连数据库）。
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserMapper userMapper;

    private AuthService authService;

    private final JwtService jwtService = new JwtServiceImpl("unit-test-secret", 2);

    @BeforeEach
    void setUp() {
        // LambdaQueryWrapper 解析 User::getUsername 依赖 MP 表信息缓存，单测环境手动初始化
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), User.class);
        authService = new AuthServiceImpl(userMapper, jwtService);
    }

    private User admin() {
        User user = new User();
        user.setId(1L);
        user.setUsername("admin");
        // SHA-256("admin123")
        user.setPasswordHash(AuthServiceImpl.sha256Hex("admin123"));
        user.setRole("ADMIN");
        return user;
    }

    @Test
    void loginSuccessIssuesToken() {
        when(userMapper.selectOne(any())).thenReturn(admin());

        LoginResponse response = authService.login("admin", "admin123");

        assertNotNull(response.token());
        assertFalse(response.token().isBlank());
        assertEquals("admin", jwtService.parseUsername(response.token()));
        assertEquals(1L, jwtService.parseUserId(response.token()));
    }

    @Test
    void loginWrongPasswordThrows() {
        when(userMapper.selectOne(any())).thenReturn(admin());

        BusinessException e = assertThrows(BusinessException.class,
                () -> authService.login("admin", "wrong-password"));

        assertEquals(1000, e.getCode());
    }

    @Test
    void loginUnknownUserThrows() {
        when(userMapper.selectOne(any())).thenReturn(null);

        BusinessException e = assertThrows(BusinessException.class,
                () -> authService.login("ghost", "admin123"));

        assertEquals(1000, e.getCode());
    }
}
