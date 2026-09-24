package com.msmeerp.onboarding.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** A company workspace the user can sign in to, as returned by "find my workspace". */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkspaceSummary {

    private String portalId;

    private String name;
}
