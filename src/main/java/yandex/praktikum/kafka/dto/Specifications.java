package yandex.praktikum.kafka.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.kafka.common.protocol.types.Field;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Specifications {

    private String weight;
    private String dimensions;
    private String batteryLife;
    private String waterResistance;
}
