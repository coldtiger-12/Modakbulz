package com.kakao.tech.spring_ai_basic.controller;

import org.springframework.ai.image.*;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class ImageController {

    private final ImageModel imageModel;

    public ImageController(ImageModel imageModel) {
        this.imageModel = imageModel;
    }

    @GetMapping("/imgGen")
    public String generateImage(@RequestParam String request) {
        System.out.println(100000);
        ImageOptions options = ImageOptionsBuilder.builder()
                .model("dall-e-3").width(1024).height(1024).build();

        ImagePrompt prompt = new ImagePrompt(request, options);
        ImageResponse response = imageModel.call(prompt);
        String imageUrl = response.getResult().getOutput().getUrl();
        System.out.println(imageUrl);
        return "redirect:" + imageUrl;
    }
}
