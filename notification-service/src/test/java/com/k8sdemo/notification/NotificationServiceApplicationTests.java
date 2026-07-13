package com.k8sdemo.notification;

import com.k8sdemo.notification.test.TestPostgresContainer;
import com.k8sdemo.notification.test.TestRabbitContainer;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import({TestPostgresContainer.class, TestRabbitContainer.class })
class NotificationServiceApplicationTests {

	@Test
	void contextLoads() {
	}

}
