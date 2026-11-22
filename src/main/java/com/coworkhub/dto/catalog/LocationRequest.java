package com.coworkhub.dto.catalog;

import com.coworkhub.validation.TimeRange;
import com.coworkhub.validation.ValidTimeRange;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalTime;

@Schema(description = "Datos para crear o actualizar una sede")
@ValidTimeRange(message = "horaApertura debe ser anterior a horaCierre")
public record LocationRequest(
        @Schema(example = "CoWork Centro") @NotBlank(message = "es obligatorio") @Size(max = 100) String nombre,
        @Schema(example = "Bucaramanga") @NotBlank(message = "es obligatorio") @Size(max = 80) String ciudad,
        @Schema(example = "Calle 35 # 19-41") @NotBlank(message = "es obligatorio") @Size(max = 200) String direccion,
        @Schema(type = "string", example = "07:00") @NotNull(message = "es obligatorio") LocalTime horaApertura,
        @Schema(type = "string", example = "20:00") @NotNull(message = "es obligatorio") LocalTime horaCierre)
        implements TimeRange<LocalTime> {

    @Override
    public LocalTime rangeStart() {
        return horaApertura;
    }

    @Override
    public LocalTime rangeEnd() {
        return horaCierre;
    }
}
