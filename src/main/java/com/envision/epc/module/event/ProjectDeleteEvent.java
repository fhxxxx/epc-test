package com.envision.epc.module.event;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * @author chaoyue.zhao1
 * @since 2025/08/11-14:55
 */
@Getter
@Setter
@ToString
@AllArgsConstructor
public class ProjectDeleteEvent {
    private Long projectId;
}
