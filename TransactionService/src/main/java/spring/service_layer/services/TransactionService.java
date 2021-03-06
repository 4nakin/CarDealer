package spring.service_layer.services;


import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import spring.repository_layer.builders.OfferBuilder;
import spring.repository_layer.models.ActionType;
import spring.repository_layer.models.Offer;
import spring.repository_layer.models.cars.FuelType;
import spring.repository_layer.models.cars.Transmission;
import spring.repository_layer.models.cars.State;
import spring.repository_layer.repositories.OfferRepository;
import spring.service_layer.dto.OfferDTO;
import spring.service_layer.dto.SearchDTO;
import spring.service_layer.dto.TransactionDTO;
import spring.web_layer.exceptions.OffersNotFoundException;

import java.util.*;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({@Autowired}))
@NoArgsConstructor
public class TransactionService {

    private OfferRepository offerRepository;
    private OfferBuilder offerBuilder;
    private TriggerService triggerService;
    private ImagesService imagesService;
    private KafkaService kafkaService;

    private static final Logger logger = LoggerFactory.getLogger(TransactionService.class);

    public List<TransactionDTO> getAllOffersBySpecifiedParams(SearchDTO searchDTO) {
        List<Offer> offersList = offerRepository.findAllByParameters
                (
                        searchDTO.getType(),
                        searchDTO.getModel(),
                        searchDTO.getMark(),
                        searchDTO.getProduction_year_from(),
                        searchDTO.getProduction_year_to(),
                        searchDTO.getState() != null ? State.valueOf(searchDTO.getState()) : null,
                        searchDTO.getFuelType() != null ? FuelType.valueOf(searchDTO.getFuelType()) : null,
                        searchDTO.getMileage_from(),
                        searchDTO.getMileage_to(),
                        searchDTO.getLowPrice(),
                        searchDTO.getHighPrice(),
                        searchDTO.getCapacity_from(),
                        searchDTO.getCapacity_to(),
                        searchDTO.getGearbox() != null ? Transmission.valueOf(searchDTO.getGearbox()) : null,
                        searchDTO.getPower_from(),
                        searchDTO.getPower_to()

                ).orElse(new LinkedList<>());

        return offersList.stream().map(TransactionDTO::new).collect(Collectors.toList());
    }


    public Long createNewOffer(OfferDTO offerDTO, Long userID) {
        try {
            Offer offer = offerBuilder.createNewOffer()
                    .title(offerDTO.getTitle())
                    .tags(offerDTO.getTags())
                    .price(offerDTO.getPrice())
                    .description(offerDTO.getDescription())
                    .carType(offerDTO.getCarType())
                    .carModel(offerDTO.getModel())
                    .productionYear(offerDTO.getProduction_year())
                    .fuelType(offerDTO.getFuelType())
                    .locatedIn(offerDTO.getLocation())
                    .withMileage(offerDTO.getMileage())
                    .withCapacity(offerDTO.getCapacity())
                    .ofPower(offerDTO.getPower())
                    .withGearboxType(offerDTO.getGearbox())
                    .withVINNumber(offerDTO.getVin())
                    .atState(offerDTO.getState())
                    .additionalEquipment(offerDTO.getAdditionalEquipment())
                    .color("RED")
                    .forUser(userID)
                    .build();

            triggerService.castDatabaseUpdate(
                    ActionType.OFFER_CREATION,
                    offerRepository.save(offer),
                    userID);

            kafkaService.notifyOfferCreation(offerDTO.setId(offer.getId()));

            return offer.getId();
        } catch (Exception exc) {
            logger.info("Error creating offer due to " + exc.getMessage());
            return null;
        }
    }

    public boolean removeOffer(Long offerId, Long userID) {
        try {
            Offer offerToBeRemoved;

            offerRepository.delete(
                    imagesService.removeImages(
                            offerToBeRemoved = offerRepository
                                    .findByUserIdAndOfferId(userID, offerId)
                                    .orElseThrow(OffersNotFoundException::new))
            );

            triggerService.castDatabaseUpdate(ActionType.OFFER_REMOVAL, offerToBeRemoved, userID);

            return true;
        } catch (Exception exc) {
            logger.error("Couldn't remove offer due to "+ exc.getMessage());
            return false;
        }
    }

    public TransactionDTO getOfferById(Long id) throws OffersNotFoundException {
        return offerRepository.findById(id).map(TransactionDTO::new).orElseThrow(OffersNotFoundException::new);
    }

    public Map<String,Float> getCheaperAndMoreExpensiveOffersRatio(Long offerId){
        Map<String,Float> ratiosMap = new HashMap<>();

        ratiosMap.put("more",(float) offerRepository.findAll().stream().filter(offer -> {
            try {
                return offer.getPrice()>offerRepository.findById(offerId)
                        .orElseThrow(OffersNotFoundException::new)
                        .getPrice();
            } catch (OffersNotFoundException e) {
                e.printStackTrace();
                return false;
            }
        }).count()/offerRepository.findAll().size());

        ratiosMap.put("less", 1-ratiosMap.get("more"));

        return ratiosMap;
    }
}
