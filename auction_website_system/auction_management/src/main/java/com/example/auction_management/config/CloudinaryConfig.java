package com.example.auction_management.config;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
public class CloudinaryConfig {
    private static Cloudinary cloudinary;

    static {
        Map<String, String> config = ObjectUtils.asMap(
                "cloud_name", "dfpd4h8an",    // thay "your_cloud_name" bằng cloud name của bạn
                "api_key", "978784727981358",          // thay "your_api_key" bằng API key của bạn
                "api_secret", "nkwOj6KsayqR7bHR9OvIKbpHzak"     // thay "your_api_secret" bằng API secret của bạn
        );
        cloudinary = new Cloudinary(config);
    }

    public static Cloudinary getCloudinary() {
        return cloudinary;
    }
}

