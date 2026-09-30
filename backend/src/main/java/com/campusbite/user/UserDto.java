package com.campusbite.user;

public record UserDto(
        Long id, String fullName, String email, String loginId, Role role,
        boolean active, boolean emailVerified, PaymentMode paymentMode,
        String department, String academicYear, String section
) {
    public static UserDto from(User u) {
        return new UserDto(u.getId(), u.getFullName(), u.getEmail(), u.getLoginId(),
                u.getRole(), u.isActive(), u.isEmailVerified(), u.getPaymentMode(),
                u.getDepartment(), u.getAcademicYear(), u.getSection());
    }
}
