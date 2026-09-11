package com.ezeiza.cartelera.service.succession;

import com.ezeiza.cartelera.entity.Plant;
import com.ezeiza.cartelera.entity.PlantMember;
import com.ezeiza.cartelera.entity.SuccessionOrder;
import com.ezeiza.cartelera.repository.PlantMemberRepository;
import com.ezeiza.cartelera.repository.SuccessionOrderRepository;
import com.ezeiza.cartelera.service.audit.AuditService;
import com.ezeiza.cartelera.service.plant.PlantPermissionService;
import com.ezeiza.cartelera.service.plant.PlantService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class SuccessionService {

    private final SuccessionOrderRepository successionOrderRepository;
    private final PlantMemberRepository plantMemberRepository;
    private final PlantService plantService;
    private final PlantPermissionService plantPermissionService;
    private final AuditService auditService;

    public SuccessionService(SuccessionOrderRepository successionOrderRepository,
                             PlantMemberRepository plantMemberRepository,
                             PlantService plantService,
                             PlantPermissionService plantPermissionService,
                             AuditService auditService) {
        this.successionOrderRepository = successionOrderRepository;
        this.plantMemberRepository = plantMemberRepository;
        this.plantService = plantService;
        this.plantPermissionService = plantPermissionService;
        this.auditService = auditService;
    }

    public List<SuccessionOrder> findActiveSuccession(String plantCode) {
        Plant plant = plantService.getActiveByCode(plantCode);

        plantPermissionService.requirePlantOperatorOrAdmin(plant.getCode());

        return successionOrderRepository.findByPlant_CodeAndActiveTrueOrderByOrderNumberAsc(plant.getCode());
    }

    public Optional<PlantMember> calculateCurrentResponsible(String plantCode) {
        Plant plant = plantService.getActiveByCode(plantCode);

        return plantMemberRepository
                .findByPlant_CodeAndActiveTrueAndAvailableTrue(plant.getCode())
                .stream()
                .filter(member -> member.getUser() != null)
                .filter(member -> member.getUser().isActive())
                .findFirst();
    }

    @Transactional
    public SuccessionOrder addToSuccession(String plantCode,
                                           Long memberId) {
        Plant plant = plantService.getActiveByCode(plantCode);

        plantPermissionService.requirePlantAdmin(plant.getCode());

        PlantMember member = plantMemberRepository
                .findByIdAndPlant_CodeAndActiveTrue(memberId, plant.getCode())
                .orElseThrow(() -> new IllegalArgumentException("Miembro de planta no encontrado"));

        if (!member.getUser().isActive()) {
            throw new IllegalArgumentException("No se puede agregar a sucesión un usuario inactivo");
        }

        Optional<SuccessionOrder> existingAnyStatus = successionOrderRepository
                .findByPlant_CodeAndPlantMember_Id(plant.getCode(), member.getId());

        if (existingAnyStatus.isPresent()) {
            SuccessionOrder existing = existingAnyStatus.get();

            if (existing.isActive()) {
                throw new IllegalArgumentException("El miembro ya está en la sucesión de esta planta");
            }

            Integer maxOrder = successionOrderRepository.findMaxActiveOrderNumberByPlantCode(plant.getCode());

            existing.setOrderNumber(maxOrder + 1);
            existing.setActive(true);

            SuccessionOrder saved = successionOrderRepository.save(existing);

            auditService.registerForPlant(
                    plant,
                    "ADD_TO_SUCCESSION",
                    "SuccessionOrder",
                    saved.getId(),
                    "Se reactivó en la sucesión a " + member.getDisplayName()
                            + " en la posición " + saved.getOrderNumber(),
                    "active=false",
                    "active=true, orderNumber=" + saved.getOrderNumber()
            );

            return saved;
        }

        Integer maxOrder = successionOrderRepository.findMaxActiveOrderNumberByPlantCode(plant.getCode());

        SuccessionOrder successionOrder = new SuccessionOrder(
                plant,
                member,
                maxOrder + 1
        );

        SuccessionOrder saved = successionOrderRepository.save(successionOrder);

        auditService.registerForPlant(
                plant,
                "ADD_TO_SUCCESSION",
                "SuccessionOrder",
                saved.getId(),
                "Se agregó a la sucesión a " + member.getDisplayName()
                        + " en la posición " + saved.getOrderNumber(),
                null,
                "memberId=" + member.getId()
                        + ", orderNumber=" + saved.getOrderNumber()
                        + ", active=true"
        );

        return saved;
    }

    @Transactional
    public void removeFromSuccession(String plantCode,
                                     Long memberId) {
        Plant plant = plantService.getActiveByCode(plantCode);

        plantPermissionService.requirePlantAdmin(plant.getCode());

        SuccessionOrder successionOrder = successionOrderRepository
                .findByPlant_CodeAndPlantMember_IdAndActiveTrue(plant.getCode(), memberId)
                .orElseThrow(() -> new IllegalArgumentException("El miembro no está en la sucesión activa"));

        PlantMember member = successionOrder.getPlantMember();

        Integer oldOrder = successionOrder.getOrderNumber();

        successionOrder.setActive(false);

        successionOrderRepository.save(successionOrder);

        normalizeSuccessionOrder(plant.getCode());

        auditService.registerForPlant(
                plant,
                "REMOVE_FROM_SUCCESSION",
                "SuccessionOrder",
                successionOrder.getId(),
                "Se quitó de la sucesión a " + member.getDisplayName(),
                "active=true, orderNumber=" + oldOrder,
                "active=false"
        );
    }

    @Transactional
    public void moveUp(String plantCode,
                       Long memberId) {
        Plant plant = plantService.getActiveByCode(plantCode);

        plantPermissionService.requirePlantOperatorOrAdmin(plant.getCode());

        SuccessionOrder current = successionOrderRepository
                .findByPlant_CodeAndPlantMember_IdAndActiveTrue(plant.getCode(), memberId)
                .orElseThrow(() -> new IllegalArgumentException("El miembro no está en la sucesión activa"));

        if (current.getOrderNumber() <= 1) {
            return;
        }

        SuccessionOrder previous = successionOrderRepository
                .findByPlant_CodeAndOrderNumberAndActiveTrue(
                        plant.getCode(),
                        current.getOrderNumber() - 1
                )
                .orElse(null);

        if (previous == null) {
            normalizeSuccessionOrder(plant.getCode());
            return;
        }

        Integer oldOrder = current.getOrderNumber();
        Integer previousOrder = previous.getOrderNumber();

        current.setOrderNumber(previousOrder);
        previous.setOrderNumber(oldOrder);

        successionOrderRepository.save(previous);
        successionOrderRepository.save(current);

        auditService.registerForPlant(
                plant,
                "MOVE_UP",
                "SuccessionOrder",
                current.getId(),
                "Se subió en la sucesión a " + current.getPlantMember().getDisplayName()
                        + " a la posición " + current.getOrderNumber(),
                "orderNumber=" + oldOrder,
                "orderNumber=" + current.getOrderNumber()
        );
    }

    @Transactional
    public void moveDown(String plantCode,
                         Long memberId) {
        Plant plant = plantService.getActiveByCode(plantCode);

        plantPermissionService.requirePlantOperatorOrAdmin(plant.getCode());

        SuccessionOrder current = successionOrderRepository
                .findByPlant_CodeAndPlantMember_IdAndActiveTrue(plant.getCode(), memberId)
                .orElseThrow(() -> new IllegalArgumentException("El miembro no está en la sucesión activa"));

        Integer maxOrder = successionOrderRepository.findMaxActiveOrderNumberByPlantCode(plant.getCode());

        if (current.getOrderNumber() >= maxOrder) {
            return;
        }

        SuccessionOrder next = successionOrderRepository
                .findByPlant_CodeAndOrderNumberAndActiveTrue(
                        plant.getCode(),
                        current.getOrderNumber() + 1
                )
                .orElse(null);

        if (next == null) {
            normalizeSuccessionOrder(plant.getCode());
            return;
        }

        Integer oldOrder = current.getOrderNumber();
        Integer nextOrder = next.getOrderNumber();

        current.setOrderNumber(nextOrder);
        next.setOrderNumber(oldOrder);

        successionOrderRepository.save(next);
        successionOrderRepository.save(current);

        auditService.registerForPlant(
                plant,
                "MOVE_DOWN",
                "SuccessionOrder",
                current.getId(),
                "Se bajó en la sucesión a " + current.getPlantMember().getDisplayName()
                        + " a la posición " + current.getOrderNumber(),
                "orderNumber=" + oldOrder,
                "orderNumber=" + current.getOrderNumber()
        );
    }

    @Transactional
    public void normalizeSuccessionOrder(String plantCode) {
        Plant plant = plantService.getActiveByCode(plantCode);

        List<SuccessionOrder> activeList = successionOrderRepository
                .findByPlant_CodeAndActiveTrueOrderByOrderNumberAsc(plant.getCode());

        int order = 1;

        for (SuccessionOrder item : activeList) {
            item.setOrderNumber(order);
            successionOrderRepository.save(item);
            order++;
        }
    }
}