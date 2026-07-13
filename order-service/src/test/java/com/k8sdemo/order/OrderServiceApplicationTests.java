package com.k8sdemo.order;

import com.k8sdemo.order.test.TestPostgresContainer;
import com.k8sdemo.order.test.TestRabbitContainer;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import({TestPostgresContainer.class, TestRabbitContainer.class })
class OrderServiceApplicationTests {

	@Test
	void contextLoads() {
	}

}
