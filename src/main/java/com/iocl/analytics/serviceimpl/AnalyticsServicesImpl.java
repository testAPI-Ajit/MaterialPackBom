package com.iocl.analytics.serviceimpl;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.iocl.analytics.dest.entity.*;
import com.iocl.analytics.dest.repo.*;
import com.iocl.analytics.dto.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iocl.analytics.constant.AnalyticsConstant;
import com.iocl.analytics.repo.AnalyticsRepo;
import com.iocl.analytics.service.AnalyticsServices;
import com.iocl.analytics.source.entity.AddtPoEntity;
import com.iocl.analytics.util.GetDate;

@Service
public class AnalyticsServicesImpl implements AnalyticsServices {
	private static final Logger logger = LogManager.getLogger(AnalyticsServicesImpl.class);
	@Autowired
	AnalyticsRepo analyticsRepo;
	@Autowired
	SendDataToAnalytics sendDataToAnalytics;
	@Autowired
	SendPackageDataToAnalytics sendPckDataToAnalytics;

	@Autowired
	SendPoImportDataToAnalytics sendPoImportDataToAnalytics;

	@Autowired
	SendPoCompetitorsDataToAnalytics sendPoCompetitorsDataToAnalytics;

	@Autowired
	MaterialBomToAnalytics materialBomToAnalyticsrepo;

	@Autowired
	PackBomToAnalytics packBomToAnalyticsrepo;

	@Autowired
	SendAddtImportsDataToAnalytics sendAddtImportsDataToAnalytics;

	//	@Autowired
//	MatBomRepo matBomRepo;
//
	@Override
	public List<AnalyticsDataDto> getDataFromHois() {

		// TODO Auto-generated method stub
		AnalyticsDataDto analyticsDataDto = null;
		List<AnalyticsDataDto> analyticsDataDtoList = new ArrayList<>();
		AddtPoEntity agrements = null;
		double rate = 0;
		double quantity = 0;
		double amount = 0;

		GetDate getDate = new GetDate();
		try {
			ObjectMapper mapper = new ObjectMapper();
//			Map<String, String> prevStartAndEndDayMap = getDate.previuosMonthDateMap();

//			String start_date = prevStartAndEndDayMap.get(AnalyticsConstant.START_DATE_OF_PREV_MONTH);
//			String end_date = prevStartAndEndDayMap.get(AnalyticsConstant.END_DATE_OF_PREV_MONTH);

			String start_date = "2022-03-10";
			String end_date = "2022-06-30";

			List<String> plantList = analyticsRepo.getPlantsByMaterialType(AnalyticsConstant.MATERIAL_TYPE);
			// List<AddtPoEntity> dataList = analyticsRepo.getDataListFromHois(plantList,
			// AnalyticsConstant.BSART_LIST);
			logger.info("plant List :: " + plantList.size());
			List<AddtPoEntity> dataList = analyticsRepo.getDataListFromHois(plantList, AnalyticsConstant.MATERIAL_TYPE,
					AnalyticsConstant.BSART_LIST, AnalyticsConstant.BSTYP_CONTRACT, start_date, end_date);

			Map<String, String> startAndEndDayMap = getDate.neaxtMonthDateMap();
			SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
			logger.info("data List :: " + mapper.writeValueAsString(dataList));
			for (AddtPoEntity poEntity : dataList) {
				analyticsDataDto = new AnalyticsDataDto();

				List<String> getCurrency = analyticsRepo.currencyList(poEntity.getEbeln(), PageRequest.of(0, 1));
				if (getCurrency != null && getCurrency.size() > 0) {
					analyticsDataDto.setCurrency(getCurrency.get(0));
				}

				analyticsDataDto.setValid_from(
						dateFormat.parse(startAndEndDayMap.get(AnalyticsConstant.START_DATE_OF_NEXT_MONTH)));
				analyticsDataDto
						.setValid_to(dateFormat.parse(startAndEndDayMap.get(AnalyticsConstant.END_DATE_OF_NEXT_MONTH)));

				analyticsDataDto.setProduct_code(poEntity.getMatnr());
				analyticsDataDto.setBlending_plant(poEntity.getWerks());
				analyticsDataDto.setDoc_no(poEntity.getEbeln());
				analyticsDataDto.setItem_no(poEntity.getEbelp());

//				List<AddtPoEntity> agrementList = analyticsRepo.getAgreement(poEntity.getEbeln(), poEntity.getEbelp());
//				logger.info("agrementList size :: " + agrementList.size());
//				if (agrementList != null && agrementList.size() > 0) {
//					agrements = agrementList.get(0);
//					analyticsDataDto.setDoc_no(agrements.getEbeln());
//					analyticsDataDto.setItem_no(agrements.getEbelp());
//					// analyticsDataDtoList.add(analyticsDataDto);
//				}

				if (analyticsDataDto.getDoc_no() != null && analyticsDataDto.getItem_no() != null
						&& analyticsDataDto.getProduct_code() != null && analyticsDataDto.getBlending_plant() != null) {
					List<AddtPoEntity> getUomQuantiyAmtList = analyticsRepo.getUomQuantiyAmt(agrements.getEbeln(),
							agrements.getEbelp(), poEntity.getMatnr(), poEntity.getWerks());
					if (getUomQuantiyAmtList != null && getUomQuantiyAmtList.size() > 0) {
						for (AddtPoEntity rateData : getUomQuantiyAmtList) {
							if (rateData.getMenge() != null && rateData.getMenge().length() > 0)
								quantity = quantity + Double.parseDouble(rateData.getMenge());
							if (rateData.getWrbtr() != null && rateData.getWrbtr().length() > 0)
								amount = amount + Double.parseDouble(rateData.getWrbtr());
							logger.info("quantity and amount :: " + quantity + " and " + amount);
						}
						if (amount > 0 && quantity > 0)
							rate = rate + (amount / quantity);
						analyticsDataDto.setBstme_uom(getUomQuantiyAmtList.get(0).getBstme());
						analyticsDataDto.setCost(rate);
					}
				}
				analyticsDataDto.setCreated_on(dateFormat.parse(startAndEndDayMap.get(AnalyticsConstant.CURRENT_DATE)));
				analyticsDataDtoList.add(analyticsDataDto);

			}
			logger.info("Possible date :: " + mapper.writeValueAsString(analyticsDataDtoList));
		} catch (Exception e) {
			logger.error("can not get data from hois analytics", e);
		}
		return analyticsDataDtoList;
	}

	@Override
	public List<CostAddTDomestic> insertResultToAnalytics(List<AnalyticsDataDto> dataFromHois) {
		CostAddTDomestic costAddTDomestic = null;
		List<CostAddTDomestic> costAddTDomesticlist = new ArrayList<>();
		List<CostAddTDomestic> returnlist = null;
		try {
			if (dataFromHois != null && dataFromHois.size() > 0) {
				for (AnalyticsDataDto dtoData : dataFromHois) {
					costAddTDomestic = new CostAddTDomestic();
					BeanUtils.copyProperties(dtoData, costAddTDomestic);
					costAddTDomesticlist.add(costAddTDomestic);

				}
				returnlist = sendDataToAnalytics.saveAll(costAddTDomesticlist);

			}
		} catch (Exception e) {
			logger.error("can not save data to analytics :: ", e);

		}
		return returnlist;

	}

	@Override
	public List<PackagingCostDto> getPackagingCostFromHois() {

		GetDate getDate = new GetDate();
		AddtPoEntity agrements = null;
		double rate = 0;
		double quantity = 0;
		double amount = 0;
		PackagingCostDto packagingCostDto = null;
		ObjectMapper mapper = new ObjectMapper();
		List<PackagingCostDto> packgingCostList = new ArrayList<>();
		try {
			List<String> plantList = analyticsRepo.getALLPlants();
			logger.info("plant List :: " + plantList.size());

//			Map<String, String> prevStartAndEndDayMap = getDate.previuosMonthDateMap();
//			String start_date = prevStartAndEndDayMap.get(AnalyticsConstant.START_DATE_OF_PREV_MONTH);
//			String end_date = prevStartAndEndDayMap.get(AnalyticsConstant.END_DATE_OF_PREV_MONTH);

			String start_date = "2022-03-10";
			String end_date = "2022-06-30";
			List<AddtPoEntity> packageDataList = analyticsRepo.getPackageDataListFromHois(plantList,
					AnalyticsConstant.PACKAGE_MATERIAL_TYPE, AnalyticsConstant.PACKAGE_MATERIAL_GRP,
					AnalyticsConstant.BSART_LIST, start_date, end_date);

			Map<String, String> startAndEndDayMap = getDate.neaxtMonthDateMap();
			SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
			for (AddtPoEntity packageEntity : packageDataList) {
				packagingCostDto = new PackagingCostDto();

				List<String> getCurrency = analyticsRepo.currencyList(packageEntity.getEbeln(), PageRequest.of(0, 1));
				if (getCurrency != null && getCurrency.size() > 0) {
					packagingCostDto.setCurrency(getCurrency.get(0));
				}
				packagingCostDto.setValid_from(
						dateFormat.parse(startAndEndDayMap.get(AnalyticsConstant.START_DATE_OF_NEXT_MONTH)));
				packagingCostDto
						.setValid_to(dateFormat.parse(startAndEndDayMap.get(AnalyticsConstant.END_DATE_OF_NEXT_MONTH)));

				packagingCostDto.setPackage_material(packageEntity.getMatnr());
				packagingCostDto.setBlending_plant(packageEntity.getWerks());
				packagingCostDto.setDoc_no(packageEntity.getEbeln());
				packagingCostDto.setItem_no(packageEntity.getEbelp());

//				List<AddtPoEntity> agrementList = analyticsRepo.getAgreement(packageEntity.getEbeln(),
//						packageEntity.getEbelp());
//				logger.info("agrementList size :: " + agrementList.size());
//				if (agrementList != null && agrementList.size() > 0) {
//					agrements = agrementList.get(0);
//					packagingCostDto.setDoc_no(agrements.getEbeln());
//					packagingCostDto.setItem_no(agrements.getEbelp());
//					// analyticsDataDtoList.add(analyticsDataDto);
//				}
				packagingCostDto.setCreated_on(dateFormat.parse(startAndEndDayMap.get(AnalyticsConstant.CURRENT_DATE)));
				if (packagingCostDto.getDoc_no() != null && packagingCostDto.getItem_no() != null
						&& packagingCostDto.getPackage_material() != null
						&& packagingCostDto.getBlending_plant() != null) {

					List<AddtPoEntity> getUomQuantiyAmtList = analyticsRepo.getUomQuantiyAmt(
							packagingCostDto.getDoc_no(), packagingCostDto.getItem_no(),
							packagingCostDto.getPackage_material(), packagingCostDto.getBlending_plant());
					if (getUomQuantiyAmtList != null && getUomQuantiyAmtList.size() > 0) {
						for (AddtPoEntity rateData : getUomQuantiyAmtList) {
							quantity = quantity + Double.parseDouble(rateData.getMenge());
							amount = amount + Double.parseDouble(rateData.getWrbtr());
							logger.info("quantity and amount :: " + quantity + " and " + amount);
						}
						rate = rate + (amount / quantity);
						packagingCostDto.setBstme_uom(getUomQuantiyAmtList.get(0).getBstme());
						packagingCostDto.setCost(rate);
					}
				}
				packgingCostList.add(packagingCostDto);

			}
			logger.info("Possible package date :: " + mapper.writeValueAsString(packgingCostList));
		} catch (Exception e) {
			logger.error("unable to fetch package cost data from hois", e);
		}
		return packgingCostList;
	}

	@Override
	public List<PackagingCost> insertCostResultToAnalytics(List<PackagingCostDto> costDataList) {
		PackagingCost packCost = null;
		List<PackagingCost> packagingCostlist = new ArrayList<>();
		List<PackagingCost> returnlist = null;
		try {
			if (costDataList != null && costDataList.size() > 0) {
				for (PackagingCostDto dtoData : costDataList) {
					packCost = new PackagingCost();
					BeanUtils.copyProperties(dtoData, packCost);
					packagingCostlist.add(packCost);

				}
				returnlist = sendPckDataToAnalytics.saveAll(packagingCostlist);

			}
		} catch (Exception e) {
			logger.error("can not save data to analytics :: ", e);

		}
		return returnlist;

	}

	@Override
	public List<PoImportsDto> getPoImportsCostFromHois() {
		GetDate getDate = new GetDate();
		double rate = 0;
		double quantity = 0;
		double amount = 0;
		PoImportsDto poImportsDto = null;
		ObjectMapper mapper = new ObjectMapper();
		List<PoImportsDto> poiCostList = new ArrayList<>();
		try {
			List<String> plantList = analyticsRepo.getPlantsByMaterialType(AnalyticsConstant.MATERIAL_TYPE_BASE); // MATERIAL_TYPE
			logger.info("plant List :: " + plantList.size());

//			Map<String, String> prevStartAndEndDayMap = getDate.previuosMonthDateMap();
//			String start_date = prevStartAndEndDayMap.get(AnalyticsConstant.START_DATE_OF_PREV_MONTH);
//			String end_date = prevStartAndEndDayMap.get(AnalyticsConstant.END_DATE_OF_PREV_MONTH);

			String start_date = "2022-03-10";
			String end_date = "2022-06-30";
			List<AddtPoEntity> poiDataList = analyticsRepo.getPoiDataListFromHois(plantList,
					AnalyticsConstant.MATERIAL_TYPE_BASE, AnalyticsConstant.BSART_LIST_PO, start_date, end_date);

			Map<String, String> startAndEndDayMap = getDate.neaxtMonthDateMap();
			SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
			for (AddtPoEntity poiEntity : poiDataList) {
				poImportsDto = new PoImportsDto();

				List<String> getCurrency = analyticsRepo.currencyList(poiEntity.getEbeln(), PageRequest.of(0, 1));
				if (getCurrency != null && getCurrency.size() > 0) {
					poImportsDto.setCurrency(getCurrency.get(0));
				}
				poImportsDto.setValid_from(
						dateFormat.parse(startAndEndDayMap.get(AnalyticsConstant.START_DATE_OF_NEXT_MONTH)));
				poImportsDto
						.setValid_to(dateFormat.parse(startAndEndDayMap.get(AnalyticsConstant.END_DATE_OF_NEXT_MONTH)));

				poImportsDto.setProduct_code(poiEntity.getMatnr());
				poImportsDto.setBlending_plant(poiEntity.getWerks());

				poImportsDto.setDoc_no(poiEntity.getEbeln());
				poImportsDto.setItem_no(poiEntity.getEbelp());
				// analyticsDataDtoList.add(analyticsDataDto);

				poImportsDto.setCreated_on(dateFormat.parse(startAndEndDayMap.get(AnalyticsConstant.CURRENT_DATE)));

				if (poImportsDto.getDoc_no() != null && poImportsDto.getItem_no() != null
						&& poImportsDto.getProduct_code() != null && poImportsDto.getBlending_plant() != null) {

					List<AddtPoEntity> getUomQuantiyAmtList = analyticsRepo.getUomQuantiyAmt(poImportsDto.getDoc_no(),
							poImportsDto.getItem_no(), poImportsDto.getProduct_code(),
							poImportsDto.getBlending_plant());
					if (getUomQuantiyAmtList != null && getUomQuantiyAmtList.size() > 0) {
						for (AddtPoEntity rateData : getUomQuantiyAmtList) {
							quantity = quantity + Double.parseDouble(rateData.getMenge());
							amount = amount + Double.parseDouble(rateData.getWrbtr());
							logger.info("quantity and amount :: " + quantity + " and " + amount);
						}
						rate = rate + (amount / quantity);
						poImportsDto.setBstme_uom(getUomQuantiyAmtList.get(0).getBstme());
						poImportsDto.setCost(rate);
					}
				}
				poiCostList.add(poImportsDto);

			}
			logger.info("Possible package date :: " + mapper.writeValueAsString(poiCostList));
		} catch (Exception e) {
			logger.error("unable to fetch package cost data from hois", e);
		}
		return poiCostList;
	}

	@Override
	public List<PoImport> insertPoiCostResultToAnalytics(List<PoImportsDto> poiCostDataList) {

		PoImport poImport = null;
		List<PoImport> poiCostlist = new ArrayList<>();
		List<PoImport> returnlist = null;
		try {
			if (poiCostDataList != null && poiCostDataList.size() > 0) {
				for (PoImportsDto poiDtoData : poiCostDataList) {
					poImport = new PoImport();
					BeanUtils.copyProperties(poiDtoData, poImport);
					poiCostlist.add(poImport);

				}
				returnlist = sendPoImportDataToAnalytics.saveAll(poiCostlist);

			}
		} catch (Exception e) {
			logger.error("can not save data to analytics :: ", e);

		}
		return returnlist;
	}

	@Override
	public List<PoCompetitorsDto> getPoCompititorsacostDataFromHois() {

		// TODO Auto-generated method stub
		PoCompetitorsDto poCompetitorsDto = null;
		List<PoCompetitorsDto> poCompetitorsDtoList = new ArrayList<>();
		AddtPoEntity agrements = null;
		double rate = 0;
		double quantity = 0;
		double amount = 0;

		GetDate getDate = new GetDate();
		try {
			ObjectMapper mapper = new ObjectMapper();
//			Map<String, String> prevStartAndEndDayMap = getDate.previuosMonthDateMap();

//			String start_date = prevStartAndEndDayMap.get(AnalyticsConstant.START_DATE_OF_PREV_MONTH);
//			String end_date = prevStartAndEndDayMap.get(AnalyticsConstant.END_DATE_OF_PREV_MONTH);

			String start_date = "2022-03-10";
			String end_date = "2022-06-30";

			List<String> plantList = analyticsRepo.getPlantsByMaterialType(AnalyticsConstant.MATERIAL_TYPE_BASE);
			// List<AddtPoEntity> dataList = analyticsRepo.getDataListFromHois(plantList,
			// AnalyticsConstant.BSART_LIST);
			logger.info("plant List :: " + plantList.size());
			List<AddtPoEntity> dataList = analyticsRepo.getDataListFromHois(plantList,
					AnalyticsConstant.MATERIAL_TYPE_BASE, AnalyticsConstant.BSART_LIST,
					AnalyticsConstant.BSTYP_CONTRACT, start_date, end_date);

			Map<String, String> startAndEndDayMap = getDate.neaxtMonthDateMap();
			SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
			logger.info("data List :: " + mapper.writeValueAsString(dataList));
			for (AddtPoEntity poEntity : dataList) {
				poCompetitorsDto = new PoCompetitorsDto();

				List<String> getCurrency = analyticsRepo.currencyList(poEntity.getEbeln(), PageRequest.of(0, 1));
				if (getCurrency != null && getCurrency.size() > 0) {
					poCompetitorsDto.setCurrency(getCurrency.get(0));
				}

				poCompetitorsDto.setValid_from(
						dateFormat.parse(startAndEndDayMap.get(AnalyticsConstant.START_DATE_OF_NEXT_MONTH)));
				poCompetitorsDto
						.setValid_to(dateFormat.parse(startAndEndDayMap.get(AnalyticsConstant.END_DATE_OF_NEXT_MONTH)));

				poCompetitorsDto.setProduct_code(poEntity.getMatnr());
				poCompetitorsDto.setBlending_plant(poEntity.getWerks());
				poCompetitorsDto.setDoc_no(poEntity.getEbeln());
				poCompetitorsDto.setItem_no(poEntity.getEbelp());

//				List<AddtPoEntity> agrementList = analyticsRepo.getAgreement(poEntity.getEbeln(), poEntity.getEbelp());
//				logger.info("agrementList size :: " + agrementList.size());
//				if (agrementList != null && agrementList.size() > 0) {
//					agrements = agrementList.get(0);
//					poCompetitorsDto.setDoc_no(agrements.getEbeln());
//					poCompetitorsDto.setItem_no(agrements.getEbelp());
//					// analyticsDataDtoList.add(analyticsDataDto);
//				}

				if (poCompetitorsDto.getDoc_no() != null && poCompetitorsDto.getItem_no() != null
						&& poCompetitorsDto.getProduct_code() != null && poCompetitorsDto.getBlending_plant() != null) {

					List<AddtPoEntity> getUomQuantiyAmtList = analyticsRepo.getUomQuantiyAmt(
							poCompetitorsDto.getDoc_no(), poCompetitorsDto.getItem_no(),
							poCompetitorsDto.getProduct_code(), poCompetitorsDto.getBlending_plant());
					if (getUomQuantiyAmtList != null && getUomQuantiyAmtList.size() > 0) {
						for (AddtPoEntity rateData : getUomQuantiyAmtList) {
							quantity = quantity + Double.parseDouble(rateData.getMenge());
							amount = amount + Double.parseDouble(rateData.getWrbtr());
							logger.info("quantity and amount :: " + quantity + " and " + amount);
						}
						rate = rate + (amount / quantity);
						poCompetitorsDto.setBstme_uom(getUomQuantiyAmtList.get(0).getBstme());
						poCompetitorsDto.setCost(rate);
					}
				}
				poCompetitorsDto.setCreated_on(dateFormat.parse(startAndEndDayMap.get(AnalyticsConstant.CURRENT_DATE)));
				poCompetitorsDtoList.add(poCompetitorsDto);

			}
			logger.info("Possible date :: " + mapper.writeValueAsString(poCompetitorsDtoList));
		} catch (Exception e) {
			logger.error("can not get data from hois analytics", e);
		}
		return poCompetitorsDtoList;
	}

	@Override
	public List<PoCompetitors> insertPocCostResultToAnalytics(List<PoCompetitorsDto> pocCostDataList) {
		PoCompetitors poCompetitors = null;
		List<PoCompetitors> pocCostlist = new ArrayList<>();
		List<PoCompetitors> returnlist = null;
		try {
			if (pocCostDataList != null && pocCostDataList.size() > 0) {
				for (PoCompetitorsDto pocDtoData : pocCostDataList) {
					poCompetitors = new PoCompetitors();
					BeanUtils.copyProperties(pocDtoData, poCompetitors);
					pocCostlist.add(poCompetitors);

				}
				returnlist = sendPoCompetitorsDataToAnalytics.saveAll(pocCostlist);

			}
		} catch (Exception e) {
			logger.error("can not save data to analytics :: ", e);

		}
		return returnlist;
	}

	@Override
	public List<AddtPoImportsDto> getAddtPoImportsFromHois() {
		GetDate getDate = new GetDate();
		double rate = 0;
		double quantity = 0;
		double amount = 0;
		AddtPoImportsDto poImportsDto = null;
		ObjectMapper mapper = new ObjectMapper();
		List<AddtPoImportsDto> poiCostList = new ArrayList<>();
		try {
			List<String> plantList = analyticsRepo.getPlantsByMaterialType(AnalyticsConstant.MATERIAL_TYPE); // MATERIAL_TYPE
			logger.info("plant List :: " + plantList.size());

//			Map<String, String> prevStartAndEndDayMap = getDate.previuosMonthDateMap();
//			String start_date = prevStartAndEndDayMap.get(AnalyticsConstant.START_DATE_OF_PREV_MONTH);
//			String end_date = prevStartAndEndDayMap.get(AnalyticsConstant.END_DATE_OF_PREV_MONTH);

			String start_date = "2022-03-10";
			String end_date = "2022-06-30";
			List<AddtPoEntity> poiDataList = analyticsRepo.getPoiDataListFromHois(plantList,
					AnalyticsConstant.MATERIAL_TYPE, AnalyticsConstant.BSART_LIST_PO, start_date, end_date);

			Map<String, String> startAndEndDayMap = getDate.neaxtMonthDateMap();
			SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
			for (AddtPoEntity poiEntity : poiDataList) {
				poImportsDto = new AddtPoImportsDto();

				List<String> getCurrency = analyticsRepo.currencyList(poiEntity.getEbeln(), PageRequest.of(0, 1));
				if (getCurrency != null && getCurrency.size() > 0) {
					poImportsDto.setCurrency(getCurrency.get(0));
				}
				poImportsDto.setValid_from(
						dateFormat.parse(startAndEndDayMap.get(AnalyticsConstant.START_DATE_OF_NEXT_MONTH)));
				poImportsDto
						.setValid_to(dateFormat.parse(startAndEndDayMap.get(AnalyticsConstant.END_DATE_OF_NEXT_MONTH)));

				poImportsDto.setMaterial_code(poiEntity.getMatnr());
				poImportsDto.setBlending_plant(poiEntity.getWerks());

				poImportsDto.setDoc_no(poiEntity.getEbeln());
				poImportsDto.setItem_no(poiEntity.getEbelp());
				// analyticsDataDtoList.add(analyticsDataDto);

				poImportsDto.setCreated_on(dateFormat.parse(startAndEndDayMap.get(AnalyticsConstant.CURRENT_DATE)));

				if (poImportsDto.getDoc_no() != null && poImportsDto.getItem_no() != null
						&& poImportsDto.getMaterial_code() != null && poImportsDto.getBlending_plant() != null) {

					List<AddtPoEntity> getUomQuantiyAmtList = analyticsRepo.getUomQuantiyAmt(poImportsDto.getDoc_no(),
							poImportsDto.getItem_no(), poImportsDto.getMaterial_code(),
							poImportsDto.getBlending_plant());
					if (getUomQuantiyAmtList != null && getUomQuantiyAmtList.size() > 0) {
						for (AddtPoEntity rateData : getUomQuantiyAmtList) {
							if (rateData.getMenge() != null && rateData.getMenge().length() > 0)
								quantity = quantity + Double.parseDouble(rateData.getMenge());
							if (rateData.getWrbtr() != null && rateData.getWrbtr().length() > 0)
								amount = amount + Double.parseDouble(rateData.getWrbtr());
							logger.info("quantity and amount :: " + quantity + " and " + amount);
						}
						if (amount > 0 && quantity > 0)
							rate = rate + (amount / quantity);
						poImportsDto.setBstme_uom(getUomQuantiyAmtList.get(0).getBstme());
						poImportsDto.setCost(rate);
					}
				}
				poiCostList.add(poImportsDto);

			}
			logger.info("Possible ADDT imports PO package data :: " + mapper.writeValueAsString(poiCostList));
		} catch (Exception e) {
			logger.error("unable to fetch package cost data from hois", e);
		}
		return poiCostList;
	}

	@Override
	public List<CostAddTImport> insertAddtPoiImportResultToAnalytics(List<AddtPoImportsDto> poiCostDataList) {

		CostAddTImport poImport = null;
		List<CostAddTImport> poiCostlist = new ArrayList<>();
		List<CostAddTImport> returnlist = null;
		try {
			if (poiCostDataList != null && poiCostDataList.size() > 0) {
				for (AddtPoImportsDto poiDtoData : poiCostDataList) {
					poImport = new CostAddTImport();
					BeanUtils.copyProperties(poiDtoData, poImport);
					poiCostlist.add(poImport);

				}
				returnlist = sendAddtImportsDataToAnalytics.saveAll(poiCostlist);

			}
		} catch (Exception e) {
			logger.error("can not save data to analytics :: ", e);

		}
		return returnlist;
	}




	// ----------------------------------Fetch HOIS Data for Pack Type-----------------
	@Override
	public List<MatPacBomDto> getPacBomFrmHois() {
		GetDate getDate = new GetDate();

		MatPacBomDto matDtoData = new MatPacBomDto();
		MatPacBomDto matUOMDataDto = new MatPacBomDto();

		ObjectMapper mapper = new ObjectMapper();

		List<MatPacBomDto> matPackBomDtoListNotEndWithThreeZero = new ArrayList<>();

		List<MatPacBomDto> matPackBomDtoListNotEndWithThreeZeroData = new ArrayList<>();
		MatPacBomDto matReturnWithNotZero = null;


		List<MatPacBomDto> matPackmatNotReturnWithZeroList = new ArrayList<>();

		try {
			PageRequest pageable = PageRequest.of(0, 500);

			Map<String, String> startAndEndDayMap = getDate.neaxtMonthDateMap();
			SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

			ObjectMapper omaperr = new ObjectMapper();
			List<MatPacBomDto> materialList = analyticsRepo.getAllMaterials();
			logger.info("List of materials from Pack table", materialList.size());

			List<String> matlist = new ArrayList<>();


			for (MatPacBomDto matDto : materialList) {
				BeanUtils.copyProperties(matDto, matDtoData);

				matDtoData.setValid_from(
						dateFormat.parse(startAndEndDayMap.get(AnalyticsConstant.START_DATE_OF_NEXT_MONTH)));
				matDtoData
						.setValid_to(dateFormat.parse(startAndEndDayMap.get(AnalyticsConstant.END_DATE_OF_NEXT_MONTH)));

				matDtoData.setCreated_on(dateFormat.parse(startAndEndDayMap.get(AnalyticsConstant.CURRENT_DATE)));
//--------------For Pack type---------------
				if (!matDtoData.getFinish_product().endsWith("000")) {

					logger.info("mat id " + matDtoData.getFinish_product());
					matPackBomDtoListNotEndWithThreeZero = analyticsRepo.getMatPackBomDto(matDtoData.getFinish_product(), matDtoData.getPlant(),pageable);

					if (matPackBomDtoListNotEndWithThreeZero != null && matPackBomDtoListNotEndWithThreeZero.size() > 0) {
						matReturnWithNotZero = transformData(matPackBomDtoListNotEndWithThreeZero, matDtoData);
						matPackBomDtoListNotEndWithThreeZeroData.add(matReturnWithNotZero);
						logger.info("Plant details in Pack " + matReturnWithNotZero);
					}
				}

			}
		} catch (Exception ex) {
			logger.error("Error while fetching PACK Data::: " + ex);
		}
		return matPackBomDtoListNotEndWithThreeZeroData;
	}

	//------------------------------------------------Fetch from HOIS for BASE/ADDT TYPE DATA----------------------------------------

	@Override
	public List<MatPacBomDto> getMatBomFrmHois(){

		GetDate getDate = new GetDate();
		List<MatPacBomDto> matPacBomDtoList = new ArrayList<>();
	//	List<MatPacBomDto> matStllknList = new ArrayList<>();

		MatPacBomDto matDtoData = new MatPacBomDto();

		MatPacBomDto matPackDto = null;
		ObjectMapper ommaperr = new ObjectMapper();
		List<MatPacBomDto> matPackBomDtoListEndWithThreeZero = new ArrayList<>();
		List<MatPacBomDto> matPackBomDtoListNotEndWithThreeZero = new ArrayList<>();
		MatPacBomDto matReturnWithZero = new MatPacBomDto();
		MatPacBomDto matReturnWithNotZero = null;

		List<MatPacBomDto> matPackmatReturnWithZeroList = new ArrayList<>();
		List<MatPacBomDto> matPackmatNotReturnWithZeroList = new ArrayList<>();
		List<MatPacBomDto> matPackBomDtoListEndWithThreeZeroData = new ArrayList<>();
		try {
			PageRequest pgReq = PageRequest.of(0, 500);
			ObjectMapper omaperr = new ObjectMapper();
			List<MatPacBomDto> materialList = analyticsRepo.getAllMaterials();
			logger.info("List of materials from Material table", materialList.size());

			List<String> matlist = new ArrayList<>();

			Map<String, String> startAndEndDayMap = getDate.neaxtMonthDateMap();
			SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

			for (MatPacBomDto matDto : materialList) {
				BeanUtils.copyProperties(matDto, matDtoData);

				matDtoData.setValid_from(
						dateFormat.parse(startAndEndDayMap.get(AnalyticsConstant.START_DATE_OF_NEXT_MONTH)));
				matDtoData.setValid_to(
						dateFormat.parse(startAndEndDayMap.get(AnalyticsConstant.END_DATE_OF_NEXT_MONTH)));

				matDtoData.setCreated_on(dateFormat.parse(startAndEndDayMap.get(AnalyticsConstant.CURRENT_DATE)));
//------------------For Material type------------
				if (matDtoData.getFinish_product().endsWith("000")) {
					logger.info("mat id for 000:::::   " + matDtoData.getFinish_product());
					logger.info("Plant ID for 000:::: " + matDtoData.getPlant());

					matPackBomDtoListEndWithThreeZero = analyticsRepo.getMatPackBomDtoForMaterialEndsWithZero(matDtoData.getFinish_product(), matDtoData.getPlant(),pgReq);

					if (matPackBomDtoListEndWithThreeZero!= null && matPackBomDtoListEndWithThreeZero.size() >0){
						matReturnWithZero = transformData(matPackBomDtoListEndWithThreeZero, matDtoData);
						matPackBomDtoListEndWithThreeZeroData.add(matReturnWithZero);
					}

				}

				}
		} catch (Exception ex) {
			logger.error(ex);
		}
		return matPackBomDtoListEndWithThreeZeroData;
	}
//
/*	@Override
	public List<MaterialBomEntity> insertMatBomDataToAnalytics(List<MaterialBomEntity> materialBom)
	{
		List<MaterialBomDto> materialBomDto = new ArrayList<>();
		return  null;
	}*/

	/*@Override
	public List<PackBomEntity> insertPackBomDataToAnalytics(List<PackBomEntity> packBom) {
		return null;
	}*/

	// Added details to fetch data for Materials and pack BOM

/*
	@Override
	public List<MatPacBomDto> getMatPacBomFrmHois() {
		List<MatPacBomDto> matPacBomDtoList = new ArrayList<>();
		List<MatPacBomDto> matStllknList = new ArrayList<>();

		MatPacBomDto matDtoData = new MatPacBomDto();
		MatPacBomDto matUOMDataDto = new MatPacBomDto();
		MatPacBomDto matPackDto = null;
		ObjectMapper mapper = new ObjectMapper();
		List<MatPacBomDto> matPackBomDtoListEndWithThreeZero = null;
		List<MatPacBomDto> matPackBomDtoListNotEndWithThreeZero = null;
		MatPacBomDto matReturnWithZero = null;
		MatPacBomDto matReturnWithNotZero = null;

		List<MatPacBomDto> matPackmatReturnWithZeroList = new ArrayList<>();
		List<MatPacBomDto> matPackmatNotReturnWithZeroList = new ArrayList<>();

		try {
			//PageRequest pgReq = PageRequest.of(0, 500);
			ObjectMapper omaperr = new ObjectMapper();
			List<MatPacBomDto> materialList = analyticsRepo.getAllMaterials();
			logger.info("List of materials from Material table", materialList.size());

			List<String> matlist = new ArrayList<>();

			// String[] materialEndSwithZero = {"BASE","ADDT"};
			// String[] materialNotEndswithZero = {"PACK"};
			for (MatPacBomDto matDto : materialList) {
				BeanUtils.copyProperties(matDto, matDtoData);
//				matDto.setFinish_product(matDto.getFinish_product());
//				matDto.setPlant(matDto.getPlant());


				if (!matDtoData.getFinish_product().endsWith("000")) {

					//logger.info("mat id " + matDtoData.getFinish_product());
					matPackBomDtoListNotEndWithThreeZero = analyticsRepo.getMatPackBomDto(matDtoData.getFinish_product(), matDtoData.getPlant());

					if(matPackBomDtoListNotEndWithThreeZero !=null && matPackBomDtoListNotEndWithThreeZero.size()>0)
					{
					matReturnWithNotZero=transformData(matPackBomDtoListNotEndWithThreeZero,matDtoData);
					matPackmatNotReturnWithZeroList.add(matReturnWithNotZero);}

				} else if (matDtoData.getFinish_product().endsWith("000")) {

						matPackBomDtoListEndWithThreeZero = analyticsRepo.getMatPackBomDtoForMaterialEndsWithZero(matDtoData.getFinish_product(), matDtoData.getPlant());


					if(matPackBomDtoListEndWithThreeZero !=null && matPackBomDtoListEndWithThreeZero.size()>0)
					{
						matReturnWithZero=transformData(matPackBomDtoListEndWithThreeZero,matDtoData);
						matPackBomDtoListEndWithThreeZero.add(matReturnWithZero);}
					//	logger.info("matPackBomDtoList size for else : " + matPackBomDtoListNotEndWithThreeZero.size());
				}
				logger.info("data transformation for PACK(NOT ENDS with 000 : " + omaperr.writeValueAsString(matPackmatNotReturnWithZeroList));
				logger.info("data transformation for PACK( ENDS with 000 : " + omaperr.writeValueAsString(matPackBomDtoListEndWithThreeZero));
			}
		} catch (Exception e) {
			logger.error(e);
		}
		return null;
	}
*/

	public MatPacBomDto transformData(List<MatPacBomDto> matPackBomDtoList, MatPacBomDto matDtoData) {
		MatPacBomDto secondTableEntity = null;

		if (matPackBomDtoList != null && matPackBomDtoList.size() > 0) {
			logger.info("Finish material details: " + matDtoData.getFinish_product());
			logger.info("Plant: " + matDtoData.getPlant());
			logger.info("matPackBomDtoList size for : " + matPackBomDtoList.size());
			if (!matPackBomDtoList.isEmpty()) {
				secondTableEntity = new MatPacBomDto();
				for (int i = 0; i < matPackBomDtoList.size(); i++) {
						secondTableEntity.setFinish_product(matDtoData.getFinish_product());
						secondTableEntity.setPlant(matDtoData.getPlant());
						secondTableEntity.setCreated_on(matDtoData.getCreated_on());
						secondTableEntity.setValid_from(matDtoData.getValid_from());
						secondTableEntity.setValid_to(matDtoData.getValid_to());
						secondTableEntity.setFnsd_by_qty(matPackBomDtoList.get(i).getFnsd_by_qty());
						secondTableEntity.setUOM(matPackBomDtoList.get(i).getUOM());

					if (i <= 15) {

						switch (i) {
							case 0:
								secondTableEntity.setSno1(matPackBomDtoList.get(i).getSno1());
								secondTableEntity.setQty1(matPackBomDtoList.get(i).getQty1());
								secondTableEntity.setUom1(matPackBomDtoList.get(i).getUom1());

								break;
							case 1:
								secondTableEntity.setSno2(matPackBomDtoList.get(i).getSno1());
								secondTableEntity.setQty2(matPackBomDtoList.get(i).getQty1());
								secondTableEntity.setUom2(matPackBomDtoList.get(i).getUom1());

								break;
							case 2:
								secondTableEntity.setSno3(matPackBomDtoList.get(i).getSno1());
								secondTableEntity.setQty3(matPackBomDtoList.get(i).getQty1());
								secondTableEntity.setUom3(matPackBomDtoList.get(i).getUom1());

								break;
							// Add cases for columns 4 to 15
							case 3:
								secondTableEntity.setSno4(matPackBomDtoList.get(i).getSno1());
								secondTableEntity.setQty4(matPackBomDtoList.get(i).getQty1());
								secondTableEntity.setUom4(matPackBomDtoList.get(i).getUom1());

								break;
							case 4:
								secondTableEntity.setSno5(matPackBomDtoList.get(i).getSno1());
								secondTableEntity.setQty5(matPackBomDtoList.get(i).getQty1());
								secondTableEntity.setUom5(matPackBomDtoList.get(i).getUom1());

								break;
							case 5:
								secondTableEntity.setSno6(matPackBomDtoList.get(i).getSno1());
								secondTableEntity.setQty6(matPackBomDtoList.get(i).getQty1());
								secondTableEntity.setUom6(matPackBomDtoList.get(i).getUom1());

								break;
							case 6:
								secondTableEntity.setSno7(matPackBomDtoList.get(i).getSno1());
								secondTableEntity.setQty7(matPackBomDtoList.get(i).getQty1());
								secondTableEntity.setUom7(matPackBomDtoList.get(i).getUom1());

								break;
							case 7:
								secondTableEntity.setSno8(matPackBomDtoList.get(i).getSno1());
								secondTableEntity.setQty8(matPackBomDtoList.get(i).getQty1());
								secondTableEntity.setUom8(matPackBomDtoList.get(i).getUom1());

								break;
							case 8:
								secondTableEntity.setSno9(matPackBomDtoList.get(i).getSno1());
								secondTableEntity.setQty9(matPackBomDtoList.get(i).getQty1());
								secondTableEntity.setUom9(matPackBomDtoList.get(i).getUom1());

								break;
							case 9:
								secondTableEntity.setSno10(matPackBomDtoList.get(i).getSno1());
								secondTableEntity.setQty10(matPackBomDtoList.get(i).getQty1());
								secondTableEntity.setUom10(matPackBomDtoList.get(i).getUom1());

								break;
							case 10:
								secondTableEntity.setSno11(matPackBomDtoList.get(i).getSno1());
								secondTableEntity.setQty11(matPackBomDtoList.get(i).getQty1());
								secondTableEntity.setUom11(matPackBomDtoList.get(i).getUom1());

								break;
							case 11:
								secondTableEntity.setSno12(matPackBomDtoList.get(i).getSno1());
								secondTableEntity.setQty12(matPackBomDtoList.get(i).getQty1());
								secondTableEntity.setUom12(matPackBomDtoList.get(i).getUom1());

								break;
							case 12:
								secondTableEntity.setSno13(matPackBomDtoList.get(i).getSno1());
								secondTableEntity.setQty13(matPackBomDtoList.get(i).getQty1());
								secondTableEntity.setUom13(matPackBomDtoList.get(i).getUom1());

								break;
							case 13:
								secondTableEntity.setSno14(matPackBomDtoList.get(i).getSno1());
								secondTableEntity.setQty14(matPackBomDtoList.get(i).getQty1());
								secondTableEntity.setUom14(matPackBomDtoList.get(i).getUom1());

								break;
							case 14:
								secondTableEntity.setSno15(matPackBomDtoList.get(i).getSno1());
								secondTableEntity.setQty15(matPackBomDtoList.get(i).getQty1());
								secondTableEntity.setUom15(matPackBomDtoList.get(i).getUom1());

								break;
							// Add more cases for additional columns as needed
							default:
								break;
						}
					}
				}
			}

		}
		return secondTableEntity;
	}


	//------Insert into Analytics-----
	@Override
	public List<MaterialBomEntity> insertMatBomDataToAnalytics(List<MatPacBomDto> materialBomList) {
		MaterialBomEntity materialBomEntity = null;

		List<MaterialBomEntity> materialBomEntityList = new ArrayList<>();
		List<MaterialBomEntity> returnlist = null;
		try {
			if (materialBomList != null && materialBomList.size() > 0) {
				for (MatPacBomDto matrPckData : materialBomList) {
					materialBomEntity = new MaterialBomEntity();
					BeanUtils.copyProperties(matrPckData, materialBomEntity);
					materialBomEntityList.add(materialBomEntity);

				}
				returnlist = materialBomToAnalyticsrepo.saveAll(materialBomEntityList);

			}
		} catch (Exception e) {
			logger.error("Can't save Material BOM Data to analytics DB:: ", e);

		}
		return returnlist;
	}


	@Override
	public List<PackBomEntity> insertPackBomDataToAnalytics(List<MatPacBomDto> packBomDataList) {
		PackBomEntity packBomEntity = null;

		List<PackBomEntity> packBomEntityList = new ArrayList<>();
		List<PackBomEntity> returnlist = null;
		try {
			if (packBomDataList != null && packBomDataList.size() > 0) {
				for (MatPacBomDto packMatData : packBomDataList) {
					packBomEntity = new PackBomEntity();
					BeanUtils.copyProperties(packMatData, packBomEntity);
					packBomEntityList.add(packBomEntity);

				}
				returnlist = packBomToAnalyticsrepo.saveAll(packBomEntityList);

			}
		} catch (Exception ex) {
			logger.error("Can't save Pack BOM Data to analytics DB:: ", ex);

		}
		return returnlist;
	}
}




/*
	@Override
	public List<BomInputentity> insertMatPacBomtoAnalytics(List<MatPacBomDto> matPacBomDtoList) {
		BomInputentity matPacBom = null;
		List<BomInputentity> matPacBomList=new ArrayList<BomInputentity>();
		List<BomInputentity> returnlist=null;
		try {
		if (matPacBomList != null && matPacBomList.size() > 0) {
				for (BomInputentity dtoData : matPacBomList) {
					matPacBom = new BomInputentity();
					BeanUtils.copyProperties(dtoData, matPacBom);
					matPacBomList.add(matPacBom);
				}
				//   returnlist = sendPckDataToAnalytics.saveAll(matPacBomList);

			}
		} catch (Exception e) {
			logger.error("Unable to save data into analytics machine :: ", e);
	}
		return returnlist;

	}
/*
public List<MaterialBomEntity> insertMatBomDataToAnalytics(List<MatPacBomDto> materialBom) {
	List<MaterialBomEntity> matBomEntity = null;
	List<MaterialBomEntity> matBomList = new ArrayList<>();
	try {
		if (matBomList != null && matBomList.size() > 0) {
			for (MaterialBomEntity matBomDto : matBomList) {
				matBomDto.setFnsh_prodct(matBomDto.getFnsh_gud_qty());
				matBomDto.setPlant(matBomDto.getPlant());
				matBomDto.setFnsh_gud_qty(matBomDto.getFnsh_gud_qty());
				matBomDto.setUom(matBomDto.getUom());
				logger.info(" Finished plant code fetched ");
				matBomDto.setMat_cmpt_code1(matBomDto.getMat_cmpt_code1());
				matBomDto.setMat_cmpt_qty1(matBomDto.getMat_cmpt_qty1());
				matBomDto.setMat_cmpt_uom1(matBomDto.getMat_cmpt_uom1());

				matBomDto.setMat_cmpt_code2(matBomDto.getMat_cmpt_code2());
				matBomDto.setMat_cmpt_qty2(matBomDto.getMat_cmpt_qty2());
				matBomDto.setMat_cmpt_uom2(matBomDto.getMat_cmpt_uom2());

				matBomDto.setMat_cmpt_code3(matBomDto.getMat_cmpt_code3());
				matBomDto.setMat_cmpt_qty3(matBomDto.getMat_cmpt_qty3());
				matBomDto.setMat_cmpt_uom3(matBomDto.getMat_cmpt_uom3());

				matBomDto.setMat_cmpt_code4(matBomDto.getMat_cmpt_code4());
				matBomDto.setMat_cmpt_qty4(matBomDto.getMat_cmpt_qty4());
				matBomDto.setMat_cmpt_uom4(matBomDto.getMat_cmpt_uom4());

				matBomDto.setMat_cmpt_code5(matBomDto.getMat_cmpt_code5());
				matBomDto.setMat_cmpt_qty5(matBomDto.getMat_cmpt_qty5());
				matBomDto.setMat_cmpt_uom5(matBomDto.getMat_cmpt_uom5());

				matBomDto.setMat_cmpt_code6(matBomDto.getMat_cmpt_code6());
				matBomDto.setMat_cmpt_qty6(matBomDto.getMat_cmpt_qty6());
				matBomDto.setMat_cmpt_uom6(matBomDto.getMat_cmpt_uom6());

				matBomDto.setMat_cmpt_code7(matBomDto.getMat_cmpt_code7());
				matBomDto.setMat_cmpt_qty7(matBomDto.getMat_cmpt_qty7());
				matBomDto.setMat_cmpt_uom7(matBomDto.getMat_cmpt_uom7());

				matBomDto.setMat_cmpt_code8(matBomDto.getMat_cmpt_code8());
				matBomDto.setMat_cmpt_qty8(matBomDto.getMat_cmpt_qty8());
				matBomDto.setMat_cmpt_uom8(matBomDto.getMat_cmpt_uom8());

				matBomDto.setMat_cmpt_code9(matBomDto.getMat_cmpt_code9());
				matBomDto.setMat_cmpt_qty9(matBomDto.getMat_cmpt_qty9());
				matBomDto.setMat_cmpt_uom9(matBomDto.getMat_cmpt_uom9());

				matBomDto.setMat_cmpt_code10(matBomDto.getMat_cmpt_code10());
				matBomDto.setMat_cmpt_qty10(matBomDto.getMat_cmpt_qty10());
				matBomDto.setMat_cmpt_uom10(matBomDto.getMat_cmpt_uom10());

				matBomDto.setMat_cmpt_code11(matBomDto.getMat_cmpt_code11());
				matBomDto.setMat_cmpt_qty11(matBomDto.getMat_cmpt_qty11());
				matBomDto.setMat_cmpt_uom11(matBomDto.getMat_cmpt_uom11());

				matBomDto.setMat_cmpt_code12(matBomDto.getMat_cmpt_code12());
				matBomDto.setMat_cmpt_qty12(matBomDto.getMat_cmpt_qty12());
				matBomDto.setMat_cmpt_uom12(matBomDto.getMat_cmpt_uom12());

				matBomDto.setMat_cmpt_code13(matBomDto.getMat_cmpt_code13());
				matBomDto.setMat_cmpt_qty13(matBomDto.getMat_cmpt_qty13());
				matBomDto.setMat_cmpt_uom13(matBomDto.getMat_cmpt_uom13());

				matBomDto.setMat_cmpt_code14(matBomDto.getMat_cmpt_code14());
				matBomDto.setMat_cmpt_qty14(matBomDto.getMat_cmpt_qty14());
				matBomDto.setMat_cmpt_uom14(matBomDto.getMat_cmpt_uom14());

				matBomDto.setMat_cmpt_code15(matBomDto.getMat_cmpt_code15());
				matBomDto.setMat_cmpt_qty15(matBomDto.getMat_cmpt_qty15());
				matBomDto.setMat_cmpt_uom15(matBomDto.getMat_cmpt_uom15());

			materialBomToAnalyticsrepo.saveAll(matBomList);

			}
		}
	} catch (Exception e) {
		logger.error("Material Bom Error: " + e);
	}
	return matBomList;
}

public List<PackBomEntity> insertPackBomDataToAnalytics(List<MatPacBomDto> packBomData)
{
	List<PackBomEntity> pack_bomEntity = null;
	List<PackBomEntity> pack_bomList = new ArrayList<>();
	try {
		if (pack_bomList != null && pack_bomList.size() > 0) {
			for (PackBomEntity packBomDto : pack_bomList) {
				packBomDto.setFnsh_prodct(packBomDto.getFnsh_gud_qty());
				packBomDto.setPlant(packBomDto.getPlant());
				packBomDto.setFnsh_gud_qty(packBomDto.getFnsh_gud_qty());
				packBomDto.setUom(packBomDto.getUom());

				packBomDto.setPack_cmpt_code1(packBomDto.getPack_cmpt_code1());
				packBomDto.setPack_cmpt_qty1(packBomDto.getPack_cmpt_code1());
				packBomDto.setPack_cmpt_uom1(packBomDto.getPack_cmpt_uom1());

				packBomDto.setPack_cmpt_code2(packBomDto.getPack_cmpt_code2());
				packBomDto.setPack_cmpt_qty2(packBomDto.getPack_cmpt_code2());
				packBomDto.setPack_cmpt_uom2(packBomDto.getPack_cmpt_uom2());

				packBomDto.setPack_cmpt_code3(packBomDto.getPack_cmpt_code3());
				packBomDto.setPack_cmpt_qty3(packBomDto.getPack_cmpt_code3());
				packBomDto.setPack_cmpt_uom3(packBomDto.getPack_cmpt_uom3());

				packBomDto.setPack_cmpt_code4(packBomDto.getPack_cmpt_code4());
				packBomDto.setPack_cmpt_qty4(packBomDto.getPack_cmpt_code4());
				packBomDto.setPack_cmpt_uom4(packBomDto.getPack_cmpt_uom4());

				packBomDto.setPack_cmpt_code5(packBomDto.getPack_cmpt_code5());
				packBomDto.setPack_cmpt_qty5(packBomDto.getPack_cmpt_code5());
				packBomDto.setPack_cmpt_uom5(packBomDto.getPack_cmpt_uom5());

				packBomDto.setPack_cmpt_code6(packBomDto.getPack_cmpt_code6());
				packBomDto.setPack_cmpt_qty6(packBomDto.getPack_cmpt_code6());
				packBomDto.setPack_cmpt_uom6(packBomDto.getPack_cmpt_uom6());

				packBomDto.setPack_cmpt_code7(packBomDto.getPack_cmpt_code7());
				packBomDto.setPack_cmpt_qty7(packBomDto.getPack_cmpt_code7());
				packBomDto.setPack_cmpt_uom7(packBomDto.getPack_cmpt_uom7());

				packBomDto.setPack_cmpt_code8(packBomDto.getPack_cmpt_code8());
				packBomDto.setPack_cmpt_qty8(packBomDto.getPack_cmpt_code8());
				packBomDto.setPack_cmpt_uom8(packBomDto.getPack_cmpt_uom8());

				packBomDto.setPack_cmpt_code9(packBomDto.getPack_cmpt_code9());
				packBomDto.setPack_cmpt_qty9(packBomDto.getPack_cmpt_code9());
				packBomDto.setPack_cmpt_uom10(packBomDto.getPack_cmpt_uom9());

				packBomDto.setPack_cmpt_code10(packBomDto.getPack_cmpt_code10());
				packBomDto.setPack_cmpt_qty10(packBomDto.getPack_cmpt_code10());
				packBomDto.setPack_cmpt_uom10(packBomDto.getPack_cmpt_uom10());

				packBomDto.setPack_cmpt_code11(packBomDto.getPack_cmpt_code11());
				packBomDto.setPack_cmpt_qty11(packBomDto.getPack_cmpt_code11());
				packBomDto.setPack_cmpt_uom11(packBomDto.getPack_cmpt_uom11());

				packBomDto.setPack_cmpt_code12(packBomDto.getPack_cmpt_code12());
				packBomDto.setPack_cmpt_qty12(packBomDto.getPack_cmpt_code12());
				packBomDto.setPack_cmpt_uom12(packBomDto.getPack_cmpt_uom12());

				packBomDto.setPack_cmpt_code13(packBomDto.getPack_cmpt_code13());
				packBomDto.setPack_cmpt_qty13(packBomDto.getPack_cmpt_code13());
				packBomDto.setPack_cmpt_uom13(packBomDto.getPack_cmpt_uom13());

				packBomDto.setPack_cmpt_code14(packBomDto.getPack_cmpt_code14());
				packBomDto.setPack_cmpt_qty14(packBomDto.getPack_cmpt_code14());
				packBomDto.setPack_cmpt_uom14(packBomDto.getPack_cmpt_uom14());

				packBomDto.setPack_cmpt_code15(packBomDto.getPack_cmpt_code15());
				packBomDto.setPack_cmpt_qty15(packBomDto.getPack_cmpt_code15());
				packBomDto.setPack_cmpt_uom15(packBomDto.getPack_cmpt_uom15());

			//	pack_bomList.add(packBomDto);
				packBomToAnalyticsrepo.saveAll(pack_bomList);
			}
		}
	} catch (Exception ex) {
		logger.error("Error in Pack Bom Service Impl : " + ex);
	}
	return pack_bomList;
	}
}
*/






