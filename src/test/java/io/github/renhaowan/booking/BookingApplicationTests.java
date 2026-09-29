package io.github.renhaowan.booking;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * 上下文启动测试：关闭 SQL 初始化、依赖 Hikari 惰性连接，保证免外部依赖可跑。
 */
@SpringBootTest(properties = "spring.sql.init.mode=never")
class BookingApplicationTests {

    @Test
    void contextLoads() {
    }
}
