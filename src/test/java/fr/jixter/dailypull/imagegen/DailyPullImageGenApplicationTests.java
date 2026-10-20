package fr.jixter.dailypull.imagegen;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {"imagegen.local-enabled=false"})
class DailyPullImageGenApplicationTests {

  @Test
  void contextLoads() {}
}
