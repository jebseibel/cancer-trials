package com.seibel.cancer.web.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * A password change for the signed-in user.
 *
 * <p>The current password is required even though the caller already holds a valid token. A JWT
 * outlives the browser tab it was issued to, so an unattended session - or a stolen token - could
 * otherwise be used to lock the real owner out of their own account. Proving knowledge of the
 * existing password is what makes that not merely a matter of timing.
 *
 * <p>There is deliberately no username field. Changing another user's password is a different
 * operation with different authorisation, and accepting a username here would make this endpoint
 * look like it supports one.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class RequestChangePassword extends BaseRequest {

    @NotBlank(message = "Your current password is required")
    private String currentPassword;

    /**
     * Minimum matches registration. Raising it here alone would leave existing accounts holding
     * passwords the app would now refuse to set - a rule that applies only to people who change
     * theirs is worse than a consistent one.
     */
    @NotBlank(message = "A new password is required")
    @Size(min = 6, message = "The new password must be at least 6 characters")
    private String newPassword;
}
