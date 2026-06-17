package com.example.library_management.dto;

public record ChatResponse(String reply, String booksJson, String keyword,boolean searchCalled ) {
}
