package com.znxsgl.converter;

import com.znxsgl.dto.ChatMessageDTO;
import com.znxsgl.entity.ChatMessage;
import org.mapstruct.Mapper;

import java.util.List;

/**
 * ChatMessage 实体 ↔ DTO 转换器
 *
 * 字段名完全一致，MapStruct 会自动映射，无需手写 @Mapping。
 * 由于 pom.xml 配置了 -Amapstruct.defaultComponentModel=spring，
 * MapStruct 会生成带 @Component 的实现类，可直接通过构造器注入使用。
 */
@Mapper
public interface ChatMessageConverter {

    ChatMessageDTO toDto(ChatMessage entity);

    List<ChatMessageDTO> toDtoList(List<ChatMessage> entities);
}
