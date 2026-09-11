package com.ezeiza.cartelera.controller.succession;

import com.ezeiza.cartelera.dto.member.PlantMemberResponse;
import com.ezeiza.cartelera.dto.succession.SuccessionOrderResponse;
import com.ezeiza.cartelera.entity.AppUser;
import com.ezeiza.cartelera.entity.Plant;
import com.ezeiza.cartelera.entity.PlantMember;
import com.ezeiza.cartelera.entity.SuccessionOrder;
import com.ezeiza.cartelera.service.succession.SuccessionService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/plants/{plantCode}/succession")
public class SuccessionController {

    private final SuccessionService successionService;

    public SuccessionController(SuccessionService successionService) {
        this.successionService = successionService;
    }

    @GetMapping
    public List<SuccessionOrderResponse> getSuccession(@PathVariable String plantCode) {
        Optional<PlantMember> currentResponsible = successionService.calculateCurrentResponsible(plantCode);

        Long currentResponsibleMemberId = currentResponsible
                .map(PlantMember::getId)
                .orElse(null);

        return successionService.findActiveSuccession(plantCode)
                .stream()
                .map(item -> toSuccessionOrderResponse(item, currentResponsibleMemberId))
                .toList();
    }

    @PostMapping("/{memberId}")
    public SuccessionOrderResponse addToSuccession(@PathVariable String plantCode,
                                                   @PathVariable Long memberId) {
        SuccessionOrder successionOrder = successionService.addToSuccession(
                plantCode,
                memberId
        );

        Optional<PlantMember> currentResponsible = successionService.calculateCurrentResponsible(plantCode);

        Long currentResponsibleMemberId = currentResponsible
                .map(PlantMember::getId)
                .orElse(null);

        return toSuccessionOrderResponse(successionOrder, currentResponsibleMemberId);
    }

    @DeleteMapping("/{memberId}")
    public void removeFromSuccession(@PathVariable String plantCode,
                                     @PathVariable Long memberId) {
        successionService.removeFromSuccession(plantCode, memberId);
    }

    @PostMapping("/{memberId}/move-up")
    public List<SuccessionOrderResponse> moveUp(@PathVariable String plantCode,
                                                @PathVariable Long memberId) {
        successionService.moveUp(plantCode, memberId);

        return getSuccession(plantCode);
    }

    @PostMapping("/{memberId}/move-down")
    public List<SuccessionOrderResponse> moveDown(@PathVariable String plantCode,
                                                  @PathVariable Long memberId) {
        successionService.moveDown(plantCode, memberId);

        return getSuccession(plantCode);
    }

    private SuccessionOrderResponse toSuccessionOrderResponse(SuccessionOrder item,
                                                              Long currentResponsibleMemberId) {
        PlantMember member = item.getPlantMember();

        return new SuccessionOrderResponse(
                item.getId(),
                item.getPlant() != null ? item.getPlant().getCode() : null,
                item.getOrderNumber(),
                member != null ? toPlantMemberResponse(member) : null,
                member != null
                        && currentResponsibleMemberId != null
                        && member.getId().equals(currentResponsibleMemberId),
                item.isActive(),
                item.getCreatedAt() != null ? item.getCreatedAt().toString() : null,
                item.getUpdatedAt() != null ? item.getUpdatedAt().toString() : null
        );
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