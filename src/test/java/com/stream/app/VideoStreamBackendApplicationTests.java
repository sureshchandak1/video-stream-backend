package com.stream.app;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.stream.app.services.VideoService;

@SpringBootTest
class VideoStreamBackendApplicationTests {

	@Autowired
	VideoService mVideoService;

	@Test
	void contextLoads() {

		mVideoService.processVideo("79a8b120-72e3-4403-9bda-bb437dd7121e");

	}

}
