package com.ezeiza.cartelera.service.member;

import com.ezeiza.cartelera.entity.AppUser;
import com.ezeiza.cartelera.entity.Plant;
import com.ezeiza.cartelera.entity.PlantMember;
import com.ezeiza.cartelera.entity.SuccessionOrder;
import com.ezeiza.cartelera.repository.AppUserRepository;
import com.ezeiza.cartelera.repository.PlantMemberRepository;
import com.ezeiza.cartelera.repository.SuccessionOrderRepository;
import com.ezeiza.cartelera.service.audit.AuditService;
import com.ezeiza.cartelera.service.plant.PlantPermissionService;
import com.ezeiza.cartelera.service.plant.PlantService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PlantMemberService {

    private final PlantMemberRepository plantMemberRepository;
    private final AppUserRepository appUserRepository;
    private final SuccessionOrderRepository successionOrderRepository;
    private final PlantService plantService;
    private final PlantPermissionService plantPermissionService;
    private final AuditService auditService;

    public PlantMemberService(PlantMemberRepository plantMemberRepository,
                              AppUserRepository appUserRepository,
                              SuccessionOrderRepository successionOrderRepository,
                              PlantService plantService,
                              PlantPermissionService plantPermissionService,
                              AuditService auditService) {
        this.plantMemberRepository = plantMemberRepository;
        this.appUserRepository = appUserRepository;
        this.successionOrderRepository = successionOrderRepository;
        this.plantService = plantService;
        this.plantPermissionService = plantPermissionService;
        this.auditService = auditService;
    }

    public List<PlantMember> findActiveMembers(String plantCode) {
        Plant plant = plantService.getActiveByCode(plantCode);

        plantPermissionService.requirePlantOperatorOrAdmin(plant.getCode());

        return plantMemberRepository.findByPlantAndActiveTrueOrderByUser_FullNameAsc(plant);
    }

    public List<PlantMember> findAllMembersByPlant(String plantCode) {
        Plant plant = plantService.getActiveByCode(plantCode);

        plantPermissionService.requirePlantAdmin(plant.getCode());

        return plantMemberRepository.findByPlant_CodeOrderByUser_FullNameAsc(plant.getCode());
    }

    public PlantMember getActiveMember(String plantCode, Long memberId) {
        Plant plant = plantService.getActiveByCode(plantCode);

        plantPermissionService.requirePlantOperatorOrAdmin(plant.getCode());

        return plantMemberRepository.findByIdAndPlant_CodeAndActiveTrue(memberId, plant.getCode())
                .orElseThrow(() -> new IllegalArgumentException("Miembro de planta no encontrado"));
    }

    @Transactional
    public PlantMember addMember(String plantCode,
                                 Long userId,
                                 String position) {
        Plant plant = plantService.getActiveByCode(plantCode);

        plantPermissionService.requirePlantAdmin(plant.getCode());

        AppUser user = getUserOrThrow(userId);

        if (!user.isActive()) {
            throw new IllegalArgumentException("No se puede agregar un usuario inactivo como miembro de planta");
        }

        PlantMember savedMember = plantMemberRepository.findByPlantAndUser(plant, user)
                .map(existing -> {
                    boolean wasActive = existing.isActive();
                    PlantMember reactivated = reactivateExistingMember(existing, position);

                    auditService.registerForPlant(
                            plant,
                            wasActive ? "UPDATE_PLANT_MEMBER" : "ADD_PLANT_MEMBER",
                            "PlantMember",
                            reactivated.getId(),
                            wasActive
                                    ? "Se actualizó el miembro de planta " + reactivated.getDisplayName()
                                    : "Se reactivó como miembro de planta a " + reactivated.getDisplayName(),
                            "active=" + wasActive,
                            "active=true, position=" + reactivated.getPosition()
                    );

                    return reactivated;
                })
                .orElseGet(() -> {
                    PlantMember created = createNewMember(plant, user, position);

                    auditService.registerForPlant(
                            plant,
                            "ADD_PLANT_MEMBER",
                            "PlantMember",
                            created.getId(),
                            "Se agregó como miembro de planta a " + created.getDisplayName()
                                    + " en " + plant.getCode(),
                            null,
                            "userId=" + user.getId()
                                    + ", position=" + created.getPosition()
                                    + ", available=" + created.isAvailable()
                                    + ", active=" + created.isActive()
                    );

                    return created;
                });

        return savedMember;
    }

    @Transactional
    public PlantMember updateMember(String plantCode,
                                    Long memberId,
                                    String position,
                                    Boolean active) {
        Plant plant = plantService.getActiveByCode(plantCode);

        plantPermissionService.requirePlantAdmin(plant.getCode());

        PlantMember member = plantMemberRepository.findByIdAndPlant_Code(memberId, plant.getCode())
                .orElseThrow(() -> new IllegalArgumentException("Miembro de planta no encontrado"));

        String oldValue = "position=" + member.getPosition()
                + ", available=" + member.isAvailable()
                + ", active=" + member.isActive();

        if (position != null) {
            member.setPosition(position);
        }

        if (active != null) {
            if (active && !member.getUser().isActive()) {
                throw new IllegalArgumentException("No se puede activar un miembro con usuario global inactivo");
            }

            member.setActive(active);

            if (!active) {
                member.setAvailable(false);
                deactivateSuccessionIfActive(plant, member);
            }
        }

        PlantMember savedMember = plantMemberRepository.save(member);

        String newValue = "position=" + savedMember.getPosition()
                + ", available=" + savedMember.isAvailable()
                + ", active=" + savedMember.isActive();

        auditService.registerForPlant(
                plant,
                "UPDATE_PLANT_MEMBER",
                "PlantMember",
                savedMember.getId(),
                "Se actualizó el miembro de planta " + savedMember.getDisplayName(),
                oldValue,
                newValue
        );

        return savedMember;
    }

    @Transactional
    public void removeMember(String plantCode,
                             Long memberId) {
        Plant plant = plantService.getActiveByCode(plantCode);

        plantPermissionService.requirePlantAdmin(plant.getCode());

        PlantMember member = plantMemberRepository.findByIdAndPlant_CodeAndActiveTrue(memberId, plant.getCode())
                .orElseThrow(() -> new IllegalArgumentException("Miembro de planta no encontrado"));

        String oldValue = "active=" + member.isActive()
                + ", available=" + member.isAvailable()
                + ", position=" + member.getPosition();

        member.setActive(false);
        member.setAvailable(false);

        deactivateSuccessionIfActive(plant, member);

        plantMemberRepository.save(member);

        auditService.registerForPlant(
                plant,
                "REMOVE_PLANT_MEMBER",
                "PlantMember",
                member.getId(),
                "Se quitó como miembro de planta a " + member.getDisplayName(),
                oldValue,
                "active=false, available=false, position=" + member.getPosition()
        );
    }

        @Transactional
    public PlantMember updateAvailability(String plantCode,
                                          Long memberId,
                                          boolean available) {
        Plant plant = plantService.getActiveByCode(plantCode);

        plantPermissionService.requirePlantOperatorOrAdmin(plant.getCode());

        PlantMember member = plantMemberRepository.findByIdAndPlant_CodeAndActiveTrue(memberId, plant.getCode())
                .orElseThrow(() -> new IllegalArgumentException("Miembro de planta no encontrado"));

        if (!member.getUser().isActive()) {
            throw new IllegalArgumentException("No se puede cambiar disponibilidad de un usuario inactivo");
        }

        boolean oldAvailability = member.isAvailable();

        if (available) {
            boolean availableInAnotherPlant = plantMemberRepository
                    .existsByUser_IdAndAvailableTrueAndActiveTrueAndPlant_CodeNot(
                            member.getUser().getId(),
                            plant.getCode()
                    );

            if (availableInAnotherPlant) {
                throw new IllegalArgumentException(
                        member.getDisplayName() + " ya está marcado como encargado en otra planta. "
                                + "Primero hay que sacarlo de ahí antes de asignarlo acá."
                );
            }

            plantMemberRepository.findByPlant_CodeAndActiveTrueAndAvailableTrue(plant.getCode())
                    .stream()
                    .filter(other -> !other.getId().equals(member.getId()))
                    .forEach(previousResponsible -> {
                        previousResponsible.setAvailable(false);
                        plantMemberRepository.save(previousResponsible);

                        auditService.registerForPlant(
                                plant,
                                "CHANGE_AVAILABILITY",
                                "PlantMember",
                                previousResponsible.getId(),
                                "Se reemplazó como encargado a " + previousResponsible.getDisplayName()
                                        + " por " + member.getDisplayName(),
                                "available=true",
                                "available=false"
                        );
                    });
        }

        member.setAvailable(available);

        PlantMember savedMember = plantMemberRepository.save(member);

        auditService.registerForPlant(
                plant,
                "CHANGE_AVAILABILITY",
                "PlantMember",
                savedMember.getId(),
                "Se cambió la disponibilidad de " + savedMember.getDisplayName()
                        + " a " + (available ? "Disponible" : "No disponible"),
                "available=" + oldAvailability,
                "available=" + available
        );

        return savedMember;
    }

    private PlantMember createNewMember(Plant plant,
                                        AppUser user,
                                        String position) {
        PlantMember member = new PlantMember(plant, user, position);
        member.setActive(true);
        member.setAvailable(false);

        return plantMemberRepository.save(member);
    }

    private PlantMember reactivateExistingMember(PlantMember existing,
                                                 String position) {
        if (!existing.getUser().isActive()) {
            throw new IllegalArgumentException("No se puede reactivar un miembro con usuario global inactivo");
        }

        existing.setActive(true);

        if (position != null) {
            existing.setPosition(position);
        }

        return plantMemberRepository.save(existing);
    }

    private void deactivateSuccessionIfActive(Plant plant,
                                              PlantMember member) {
        successionOrderRepository
                .findByPlant_CodeAndPlantMember_IdAndActiveTrue(plant.getCode(), member.getId())
                .ifPresent(successionOrder -> {
                    successionOrder.setActive(false);
                    successionOrderRepository.save(successionOrder);
                    normalizeSuccessionOrderInternal(plant.getCode());
                });
    }

    private void normalizeSuccessionOrderInternal(String plantCode) {
        List<SuccessionOrder> activeList = successionOrderRepository
                .findByPlant_CodeAndActiveTrueOrderByOrderNumberAsc(plantCode);

        int order = 1;

        for (SuccessionOrder item : activeList) {
            item.setOrderNumber(order);
            successionOrderRepository.save(item);
            order++;
        }
    }

    private AppUser getUserOrThrow(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("El usuario es obligatorio");
        }

        return appUserRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
    }
}