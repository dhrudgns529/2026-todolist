// DTO
@Data
public class SystemDataDTO {
    private String type;
    private String system;
    private List<DateValueDTO> dataVal;
}

@Data
public class DateValueDTO {
    private String date;
    private int value;
}
