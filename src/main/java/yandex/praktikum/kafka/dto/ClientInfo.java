package yandex.praktikum.kafka.dto;

import lombok.*;
import org.apache.commons.lang3.builder.EqualsExclude;

import java.util.Objects;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClientInfo {

    private String userName;
    @EqualsAndHashCode.Exclude
    private String productName;
}
