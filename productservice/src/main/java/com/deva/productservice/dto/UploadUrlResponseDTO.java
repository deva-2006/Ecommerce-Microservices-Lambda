package com.deva.productservice.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UploadUrlResponseDTO {
    private String uploadUrl;
    private String publicUrl;
}
