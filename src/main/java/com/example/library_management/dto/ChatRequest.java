package com.example.library_management.dto;

import java.util.List;

public record ChatRequest (
        String message,
        List<MessageDTO> history){}
