package io.github.renhaowan.booking.dto;

import io.github.renhaowan.booking.domain.Room;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 会议室响应。
 */
@Schema(description = "可用会议室")
public record RoomResponse(
        @Schema(description = "会议室 id") Long id,
        @Schema(description = "名称") String name,
        @Schema(description = "容量（人）") Integer capacity,
        @Schema(description = "是否有投影仪") Boolean hasProjector,
        @Schema(description = "位置") String location,
        @Schema(description = "状态：1 可用 / 0 停用") Integer status) {

    public static RoomResponse from(Room room) {
        return new RoomResponse(room.getId(), room.getName(), room.getCapacity(),
                room.getHasProjector(), room.getLocation(), room.getStatus());
    }
}
