// 서비스
// 전체
SystemDataDTO totalDto = new SystemDataDTO();
totalDto.setType("전체");
totalDto.setSystem(null);
totalDto.setDataVal(totalRows);
result.add(totalDto);

// 시스템별
Map<String, SystemDataDTO> systemMap = new LinkedHashMap<>();
for (SystemRow row : systemRows) {
    String system = row.getSystem();

    systemMap.computeIfAbsent(system, k -> {
        SystemDataDTO dto = new SystemDataDTO();
        dto.setType("시스템");
        dto.setSystem(k);
        dto.setDataVal(new ArrayList<>());
        result.add(dto);
        return dto;
    });

    systemMap.get(system).getDataVal()
        .add(new DateValueDTO(row.getDate(), row.getValue()));
}
