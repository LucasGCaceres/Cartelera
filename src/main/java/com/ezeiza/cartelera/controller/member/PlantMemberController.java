package com.ezeiza.cartelera.controller.member;

import com.ezeiza.cartelera.dto.member.AvailabilityRequest;
import com.ezeiza.cartelera.dto.member.CreatePlantMemberRequest;
import com.ezeiza.cartelera.dto.member.PlantMemberResponse;
import com.ezeiza.cartelera.dto.member.UpdatePlantMemberRequest;
import com.ezeiza.cartelera.entity.AppUser;
import com.ezeiza.cartelera.entity.Plant;
import com.ezeiza.cartelera.entity.PlantMember;
import com.ezeiza.cartelera.service.member.PlantMemberService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/plants/{plantCode}/members")
public class PlantMemberController {

    private final PlantMemberService plantMemberService;

    public PlantMemberController(PlantMemberService plantMemberService) {
        this.plantMemberService = plantMemberService;
    }

    @GetMapping
    public List<PlantMemberResponse> getMembers(@PathVariable String plantCode) {
        return plantMemberService.findActiveMembers(plantCode)
                .stream()
                .map(this::toPlantMemberResponse)
                .toList();
    }

    /*
     * Endpoint cómodo para frontend:
     * POST /api/plants/ezeiza/members
     * body: { "userId": 10, "position": "Jefe de planta" }
     */
    @PostMapping
    public PlantMemberResponse addMember(@PathVariable String plantCode,
                                         @RequestBody CreatePlantMemberRequest request) {
        PlantMember member = plantMemberService.addMember(
                plantCode,
                request.userId(),
                request.position()
        );

        return toPlantMemberResponse(member);
    }

    /*
     * Endpoint alineado al diseño propuesto:
     * POST /api/plants/ezeiza/members/10
     * body opcional: { "userId": null, "position": "Jefe de planta" }
     *
     * Si el body viene null, igual crea el miembro con position vacío.
     */
    @PostMapping("/{userId}")
    public PlantMemberResponse addMemberByUserId(@PathVariable String plantCode,
                                                 @PathVariable Long userId,
                                                 @RequestBody(required = false) CreatePlantMemberRequest request) {
        String position = request != null ? request.position() : null;

        PlantMember member = plantMemberService.addMember(
                plantCode,
                userId,
                position
        );

        return toPlantMemberResponse(member);
    }

    @PatchMapping("/{memberId}")
    public PlantMemberResponse updateMember(@PathVariable String plantCode,
                                            @PathVariable Long memberId,
                                            @RequestBody UpdatePlantMemberRequest request) {
        PlantMember member = plantMemberService.updateMember(
                plantCode,
                memberId,
                request.position(),
                request.active()
        );

        return toPlantMemberResponse(member);
    }

    @DeleteMapping("/{memberId}")
    public void removeMember(@PathVariable String plantCode,
                             @PathVariable Long memberId) {
        plantMemberService.removeMember(plantCode, memberId);
    }

    @PatchMapping("/{memberId}/availability")
    public PlantMemberResponse updateAvailability(@PathVariable String plantCode,
                                                  @PathVariable Long memberId,
                                                  @RequestBody AvailabilityRequest request) {
        boolean available = Boolean.TRUE.equals(request.available());

        PlantMember member = plantMemberService.updateAvailability(
                plantCode,
                memberId,
                available
        );

        return toPlantMemberResponse(member);
    }

    private PlantMemberResponse toPlantMemberResponse(PlantMember member) {
        Plant plant = member.getPlant();
        AppUser user = member.getUser();

        return new PlantMemberResponse(
                member.getId(),
                plant != null ? plant.getCode() : null,
                user != null ? user.getId() : null,
                user != null ? user.getUsername() : null,
                user != null ? user.getCorporateEmail() : null,
                user != null ? user.getFullName() : null,
                member.getPosition(),
                member.isAvailable(),
                member.isActive(),
                member.getCreatedAt() != null ? member.getCreatedAt().toString() : null,
                member.getUpdatedAt() != null ? member.getUpdatedAt().toString() : null
        );
    }
}