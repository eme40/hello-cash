package com.wallet.hello_cash_wallet.payload.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
@Data
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegistrationRequest {
  private String fullName;
  @Size(min = 10, max = 10, message = "BVN must be 10 digits")
  @NotBlank(message = "Required Field")
  private String bvn;
  private LocalDate dateOfBirth;
  @Size(min = 4, max = 4, message = "BVN must be 10 digits")
  @NotBlank(message = "Required Field")
  private String pin;
  private String phoneNumber;

}
