package br.com.estapar.parkingmanagement.application.dto.error;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "DTO padrão para respostas de erro da API.")
public class ApiErrorResponseDTO {

    @Schema(description = "Timestamp de quando o erro ocorreu.", example = "2025-05-24T17:30:00.123456")
    private LocalDateTime timestamp;

    @Schema(description = "Código de status HTTP.", example = "404")
    private int status;

    @Schema(description = "Breve descrição do erro HTTP.", example = "Not Found")
    private String error;

    @Schema(description = "Mensagem detalhada sobre o erro.", example = "Veículo não encontrado ou não possui um registro de estacionamento ativo.")
    private String message;

    @Schema(description = "Caminho da requisição que originou o erro.", example = "/api/v1/plate-status")
    private String path;
}