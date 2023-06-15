/**
@Author      Sudheer,G01304428
@name        CG_CreditAppTriggerHandler
@CreateDate  2020-05-05
@Description Handler for the Trigger on Credit Application Object
@Version     <1.0>
@reference 
*/

public with sharing class CG_CreditAppTriggerHandler {
    
    public static Boolean runOnce = TRUE;
    
    /**
*  Description    Method called from after insert trigger.
*  @name          createDecRec
*  @param 		  List<LLC_BI__Product_Package__c> ppList
*  @return 		  Void
*  @throws exception NA
*/    
    
    public static void createDecRec (List<LLC_BI__Product_Package__c> ppList){
        
        List<sObject> decRecList = new List<sObject>();
        CG_Decision_History__c decObj;
        
        
        for(LLC_BI__Product_Package__c ppRec : ppList){
            if(ppRec.Is_Digital_Application__c != true){
                decObj = new CG_Decision_History__c();
                decObj.Credit_Application__c=ppRec.Id;
                
                decRecList.add(decObj); 
            }
        }       
        
        try{
            
            if(decRecList.size()>0){
                Database.SaveResult[] srInsertList = Database.insert(decRecList,false); 
                ATH_LogHandler.logSaveResult('CG_CreditAppTriggerHandler', 'createDecRec', srInsertList);
                
            }
        }
        catch(Exception e){
            ATH_LogHandler.logException(e, 'CG_CreditAppTriggerHandler', 'createDecRec', 'CreditApplication');
        }
        
    }
    /**
*  Description    This method will be called by Trigger before insert
*  @name          restrictNonBPALFacilities   
*  @param 		  List<LLC_BI__Loan__c> loanList
*  @return 		  Void
*  @throws exception NA
*/    
    public static void restrictNonBPALFacilities(List<LLC_BI__Loan__c> loanList){
        
        Set<Id> prodPkgIdSet = new Set<Id>();
        Set<String> loantoValidateType = new Set<String>(); 
        Set<String> loantoValidateProduct = new Set<String>(); 
        Set<Id> caBPALIDSet = new Set<Id>();
        
        Integer totalLoansAmount = 0;
        Integer bPALimit = 0;
        Integer modLoanCount =0;
        Integer modODCount =0;
        Integer loanFacilityCount =0;
        Integer odfacilityCount =0;
        
        for(LLC_BI__Loan__c loan : loanList){
            prodPkgIdSet.add(loan.LLC_BI__Product_Package__c);
        }
        
        
        
        if(!prodPkgIdSet.isEmpty()){
            
            for(LLC_BI__Loan__c loan : [Select Id,LLC_BI__lookupKey__c,LLC_BI__Stage__c,LLC_BI__Is_Modification__c,LLC_BI__Product_Package__c,LLC_BI__Product_Package__r.BPAL_Limit__c,LLC_BI__Amount__c,LLC_BI__Product_Line__c,LLC_BI__Product__c,LLC_BI__Product_Type__c From LLC_BI__Loan__c
                                        where LLC_BI__Product_Type__c IN('Overdraft','Loan') and LLC_BI__Product_Package__r.Application_Purpose__c ='2A' and LLC_BI__Product_Package__c =:prodPkgIdSet]){
                                            caBPALIDSet.add(loan.LLC_BI__Product_Package__c);
                                            loantoValidateType.add(loan.LLC_BI__Product_Type__c);
                                            loantoValidateProduct.add(loan.LLC_BI__Product__c);
                                            if(loan.LLC_BI__Product_Package__r.BPAL_Limit__c != null){
                                                bPALimit = Integer.valueOf(loan.LLC_BI__Product_Package__r.BPAL_Limit__c);
                                            }
                                            if(loan.LLC_BI__Amount__c != null){
                                                totalLoansAmount = totalLoansAmount + Integer.valueOf(loan.LLC_BI__Amount__c);
                                            }
                                            if(loan.LLC_BI__Is_Modification__c == false && loan.LLC_BI__Stage__c != CG_Constants.LOAN_STAGE_COMP 
                                               && (loan.LLC_BI__Product__c == CG_Constants.PRODUCT_BBO25 || loan.LLC_BI__Product__c == CG_Constants.PRODUCT_BBUT25)){
                                                   loanFacilityCount = loanFacilityCount + 1;
                                               }
                                            //modified below as part of GRPCONLEND-22921
                                            if(loan.LLC_BI__Product_Type__c == CG_Constants.PRODUCT_TYPE_OVERDRAFT
                                               && loan.LLC_BI__Stage__c != CG_Constants.LOAN_STAGE_COMP){
                                                   odfacilityCount = odfacilityCount + 1;
                                               }
                                            
                                            if(loan.LLC_BI__lookupKey__c != null  && loan.LLC_BI__Is_Modification__c == true && loan.LLC_BI__Stage__c != CG_Constants.LOAN_STAGE_COMP
                                               && (loan.LLC_BI__Product__c == CG_Constants.PRODUCT_BBO25 || loan.LLC_BI__Product__c == CG_Constants.PRODUCT_BBUT25)){
                                                   modLoanCount = modLoanCount + 1;
                                               }
                                            
                                        }  
            
            
        }
        
        for(LLC_BI__Loan__c loan : loanList){
            if(!caBPALIDSet.isEmpty() && caBPALIDSet.contains(loan.LLC_BI__Product_Package__c)){
                //modified below if  as part of GRPCONLEND-22921
                if(loan.LLC_BI__Product_Type__c == CG_Constants.PRODUCT_TYPE_OVERDRAFT && odfacilityCount >= 1 
                   && loantoValidateType.contains(loan.LLC_BI__Product_Type__c)){
                       loan.addError(CG_Constants.ERROR_BPAL);
                   }
                
                if(loan.LLC_BI__Product_Type__c == CG_Constants.PRODUCT_TYPE_LOAN && loanFacilityCount >= 1 &&  loan.LLC_BI__Is_Modification__c == false
                   && loantoValidateType.contains(loan.LLC_BI__Product_Type__c) && (loan.LLC_BI__Product__c == CG_Constants.PRODUCT_BBO25 || loan.LLC_BI__Product__c == CG_Constants.PRODUCT_BBUT25)){
                       loan.addError(CG_Constants.ERROR_BPAL);
                   }
                else if(loan.LLC_BI__Product_Type__c == CG_Constants.PRODUCT_TYPE_LOAN && modLoanCount >= 2 && loan.LLC_BI__Is_Modification__c == true 
                        && loantoValidateType.contains(loan.LLC_BI__Product_Type__c) && (loan.LLC_BI__Product__c == CG_Constants.PRODUCT_BBO25 || loan.LLC_BI__Product__c == CG_Constants.PRODUCT_BBUT25)){
                            loan.addError(CG_Constants.ERROR_BPAL);
                        }
                
                
                
            }
        }
        
    }
    /**
*  Description    This method will be called by Trigger before update
*  @name          restrictNonBPALFacilities   
*  @param 		  List<LLC_BI__Product_Package__c> creditAppList,Map<Id,LLC_BI__Product_Package__c> oldCAMap
*  @return 		  Void
*  @throws exception NA
*/    
    
    public static void validateBPALCreditApp(List<LLC_BI__Product_Package__c> creditAppList,Map<Id,LLC_BI__Product_Package__c> oldCAMap){
        
        Integer loanTypeCounter = 0, overdraftTypeCounter = 0, nonBpalProdCounter = 0,nonBpalProdCounter1=0,assetFinTypeCounter = 0; 
        
        Set<Id> prodPkgIdSet = new Set<Id>();
        Set<Id> loantoValidate = new Set<Id>();
        Set<Id> creditAppIdSet = new Set<Id>();
        Set<Id> result = new Set<Id>();
        
        //GRPCONLEND-22407 - Ganesh Gawali - START
        //Renewal,Customer Appeal,Internal Appeal,Existing Customer – Modifications Credit Referral,
        //Existing Customer – Modifications Self Sanction,Collateral Modification {'3A','4A','4B','6A','7A','8A'}
        Set<String> setOfAppPurposes = new Set<String>(Label.ATH_AppPurposeF41.split(','));
        Set<Id> setOfFilteredCredAppIds = new Set<Id>();
        //GRPCONLEND-22407 - Ganesh Gawali - END
        
        for (LLC_BI__Product_Package__c singleCARec : creditAppList){
            
            if((singleCARec.Application_Purpose__c != oldCAMap.get(singleCARec.Id).Application_Purpose__c) && 
               (singleCARec.Application_Purpose__c == CG_Constants.BPAL_APP_PURPOSE  || 
                setOfAppPurposes.contains(singleCARec.Application_Purpose__c))){
                    prodPkgIdSet.add(singleCARec.Id); 
                }
            //CHANGES AS PER BUKBBSF -11157
            if(singleCARec.Application_Purpose__c==CG_Constants.BPAL_APP_PURPOSE && singleCARec.Date_of_Previous_Pricing_Call__c!=null)
            {
                singleCARec.BPAL_Flag__c = CG_Constants.BPAL_FLAG_B;
            }
            
            //SonarQube Fixes - Ganesh Gawali - If condition combined - Changes Start
            if(oldCAMap!=null && singleCARec.Application_Purpose__c==CG_Constants.FULLCREDIT_APP_PURPOSE && oldCAMap.get(singleCARec.Id).Application_Purpose__c==CG_Constants.BPAL_APP_PURPOSE && singleCARec.Date_of_Previous_Pricing_Call__c!=null &&  singleCARec.BPAL_Flag__c=='B'){
                singleCARec.BPAL_Flag__c = CG_Constants.BPAL_FLAG_F;
            }
            if(oldCAMap!=null && singleCARec.Application_Purpose__c==CG_Constants.BPAL_APP_PURPOSE && singleCARec.Status__c==CG_Constants.STATUS_WITHDRAWN  && singleCARec.Date_of_Previous_Pricing_Call__c!=null){
                singleCARec.BPAL_Flag__c = CG_Constants.BPAL_FLAG_E;
            }
            //CHANGES AS PER GRPCONLEND-21816
            if(oldCAMap!=null && singleCARec.Application_Purpose__c!=oldCAMap.get(singleCARec.Id).Application_Purpose__c && singleCARec.Application_Purpose__c!=CG_Constants.FULLCREDIT_APP_PURPOSE && oldCAMap.get(singleCARec.Id).Application_Purpose__c==CG_Constants.BPAL_APP_PURPOSE && singleCARec.Status__c==CG_Constants.STATUS_PENDING  && singleCARec.Date_of_Previous_Pricing_Call__c!=null && singleCARec.BPAL_Flag__c!=null){
                singleCARec.addError(CG_Constants.ERROR_APPLICATIONPURPOSE_CHANGE);
            }
            //SonarQube Fixes - Ganesh Gawali - If condition combined - Changes End
            
            //Added validation to prevent users from updating application purpose to collateral modification when there is new land collateral as part of GRPCONLEND-22161
            if(singleCARec.Application_Purpose__c!=oldCAMap.get(singleCARec.Id).Application_Purpose__c && singleCARec.Application_Purpose__c == CG_Constants.APPLICATION_PURPOSE_COLLATERAL_MODIFICATION){
                creditAppIdSet.add(singleCARec.Id);
            }
            
            //GRPCONLEND-22695
            if(runOnce && singleCARec.Is_Application_Return_to_Sender__c!= null && singleCARec.Is_Application_Return_to_Sender__c == CG_Constants.IS_APPLICATION_RETURN_TO_SENDER
               && singleCARec.Is_Application_Return_to_Sender__c !=oldCAMap.get(singleCARec.Id).Is_Application_Return_to_Sender__c){
                   if(singleCARec.ATH_Return_To_Sender_Counter__c != null){
                       singleCARec.ATH_Return_To_Sender_Counter__c  += 1;
                   }
                   else{
                       singleCARec.ATH_Return_To_Sender_Counter__c = 1;
                   }
                   runOnce = FALSE;
               }                
            
            
        }
        
        //GRPCONLEND-22161
        if(!creditAppIdSet.isEmpty()){
            result = CG_CreditAppTriggerHandlerHelper.vadilateLandCollateralsInCA(creditAppIdSet);
        }
        //GRPCONLEND-22161 Ends above
        if(!prodPkgIdSet.isEmpty()){
            for(LLC_BI__Loan__c loan : [SELECT Id,LLC_BI__lookupKey__c,LLC_BI__Product_Package__r.Application_Purpose__c,LLC_BI__Is_Modification__c,LLC_BI__Product_Package__c,LLC_BI__Amount__c,LLC_BI__Product_Line__c,LLC_BI__Product__c,LLC_BI__Product_Type__c 
                                        FROM LLC_BI__Loan__c
                                        WHERE LLC_BI__Product_Package__c =: prodPkgIdSet]){
                                            //SonarQube Fixes - Ganesh Gawali - If condition combined - Changes Start  
                                            if(CG_Constants.BPAL_APP_PURPOSE == loan.LLC_BI__Product_Package__r.Application_Purpose__c &&
                                               loan.LLC_BI__Product_Type__c == CG_Constants.PRODUCT_TYPE_OVERDRAFT){
                                                   loantoValidate.add(loan.Id);
                                                   overdraftTypeCounter++;
                                               }
                                            else if(CG_Constants.BPAL_APP_PURPOSE == loan.LLC_BI__Product_Package__r.Application_Purpose__c &&
                                                    (loan.LLC_BI__Product_Type__c == CG_Constants.PRODUCT_TYPE_LOAN && loan.LLC_BI__Product__c == CG_Constants.PRODUCT_BBO25 || loan.LLC_BI__Product__c == CG_Constants.PRODUCT_BBUT25) ){
                                                        loantoValidate.add(loan.Id);
                                                        loanTypeCounter++;
                                                    }
                                            else if(CG_Constants.BPAL_APP_PURPOSE == loan.LLC_BI__Product_Package__r.Application_Purpose__c &&
                                                    loan.LLC_BI__Product_Type__c == CG_Constants.PRODUCT_TYPE_AF && (loan.LLC_BI__Product__c == CG_Constants.PRODUCT_TYPE_FL || loan.LLC_BI__Product__c == CG_Constants.PRODUCT_TYPE_HP)){
                                                        loantoValidate.add(loan.Id);
                                                        assetFinTypeCounter++;
                                                    }
                                            else if(CG_Constants.BPAL_APP_PURPOSE == loan.LLC_BI__Product_Package__r.Application_Purpose__c &&
                                                    loan.LLC_BI__Product_Type__c == CG_Constants.PRODUCT_TYPE_LOAN && (loan.LLC_BI__Product__c == CG_Constants.PRODUCT_AGRICULTURAL_MORTGAGE || loan.LLC_BI__Product__c == CG_Constants.PRODUCT_COMMERCIAL_MORTGAGE)){
                                                        loantoValidate.add(loan.Id);      
                                                        nonBpalProdCounter++;
                                                    }
                                            else if(CG_Constants.BPAL_APP_PURPOSE == loan.LLC_BI__Product_Package__r.Application_Purpose__c &&
                                                    (loan.LLC_BI__Product_Type__c == CG_Constants.PRODUCT_TYPE_SPLLOAN || loan.LLC_BI__Product_Type__c == CG_Constants.PRODUCT_TYPE_CRSL || loan.LLC_BI__Product_Type__c == CG_Constants.PRODUCT_TYPE_TF ||
                                                     loan.LLC_BI__Product_Type__c ==  CG_Constants.PRODUCT_TYPE_TF || loan.LLC_BI__Product_Type__c == CG_Constants.PRODUCT_TYPE_SL || loan.LLC_BI__Product_Type__c == CG_Constants.PRODUCT_TYPE_SF || loan.LLC_BI__Product_Type__c == CG_Constants.PRODUCT_TYPE_SFET)){
                                                         loantoValidate.add(loan.Id);      
                                                         nonBpalProdCounter1++;
                                                     }
                                            //SonarQube Fixes - Ganesh Gawali - If condition combined - Changes End
                                            
                                            //GRPCONLEND-22407 - Ganesh Gawali - START
                                            if(FALSE == loan.LLC_BI__Is_Modification__c && 
                                               NULL != loan.LLC_BI__Product_Package__r.Application_Purpose__c){
                                                   setOfFilteredCredAppIds.add(loan.LLC_BI__Product_Package__c);
                                               }//GRPCONLEND-22407 - Ganesh Gawali - END
                                        }
            
        }
        
        for(LLC_BI__Product_Package__c pp : creditAppList){
            
            //SonarQube Fixes - Ganesh Gawali - If condition combined - Changes Start
            if(!loantoValidate.isEmpty() && pp.Application_Purpose__c == CG_Constants.BPAL_APP_PURPOSE &&
               (overdraftTypeCounter > 1 || loanTypeCounter > 1 || assetFinTypeCounter > 1 || nonBpalProdCounter > 0 || nonBpalProdCounter1 > 0)){
                   pp.addError(CG_Constants.ERROR_BPAL);   
               }
            else if(!loantoValidate.isEmpty() && 
                    (pp.Application_Purpose__c == CG_Constants.BPAL_APP_PURPOSE && pp.LLC_BI__Total_Loan_Facilities_Amount__c > pp.BPAL_Limit__c)){
                        pp.addError(CG_Constants.ERROR_BPAL); 
                    }
            //SonarQube Fixes - Ganesh Gawali - If condition combined - Changes End
            
            if(!result.isEmpty() && result.contains(pp.id)){
                pp.addError(Label.ATH_Error_Message_in_CA_for_Collateral_Modification);
            }
            //GRPCONLEND-22407 - Ganesh Gawali - START
            if(!setOfFilteredCredAppIds.isEmpty() && setOfFilteredCredAppIds.contains(pp.id)){
                pp.addError(Label.ATH_CredAppErrorF41);
            }//GRPCONLEND-22407 - Ganesh Gawali - END
        }
    }
    
    /**
*  Description    This method updates all facilities SFG flag for BPAL CA
*  @name          updateLoanSFGForBPALCA   
*  @param 		  List<LLC_BI__Product_Package__c> creditAppList,Map<Id,LLC_BI__Product_Package__c> oldCAMap
*  @return 		  Void
*  @throws exception NA
*/    
    public static void updateLoanSFGForBPALCA(List<LLC_BI__Product_Package__c> creditAppList,Map<Id,LLC_BI__Product_Package__c> oldCAMap){
        Set<Id> setOfCAToGetLoan = new Set<Id> ();
        //GRPCONLEND-21120
        Set<Id> creditAppIdSetRePriceFlag = new Set<Id> ();
        for(LLC_BI__Product_Package__c creditApp:creditAppList){
            
            if(creditApp.Application_Entity__c == CG_Constants.APPLICATION_ENTITY && creditApp.Secure_By_Sfg_Flag__c !=(oldCAMap.get(creditApp.Id)).Secure_By_Sfg_Flag__c  ){
                setOfCAToGetLoan.add(creditApp.Id);
            }
            //GRPCONLEND-21120
            if(creditApp.Application_Entity__c == CG_Constants.APPLICATION_ENTITY &&((creditApp.Risk_Grade__c !=(oldCAMap.get(creditApp.Id)).Risk_Grade__c&& creditApp.Risk_Grade__c==creditApp.BUK_Risk_Grade__c)||
                                                                                     (creditApp.Proposed_Also_Secures_Value__c !=(oldCAMap.get(creditApp.Id)).Proposed_Also_Secures_Value__c)) ){
                                                                                         creditAppIdSetRePriceFlag.add(creditApp.Id);
                                                                                     }           
            
        }
        if(!setOfCAToGetLoan.isEmpty()){
            updateLoanSFGForCA(setOfCAToGetLoan);
        }
        //GRPCONLEND-21120
        if(!creditAppIdSetRePriceFlag.isEmpty()){
            CG_LoanTriggerHandler.updateRepriceFlagOnCA(creditAppIdSetRePriceFlag);
        }
        
    }
    
    /**
*  Description    This method updates New Collateral flag for  CA
*  @name          updateNewCollateralFlag   
*  @param 		  Set<Id> creditAppIdSet
*  @return 		  Void
*  @throws exception NA
*/   
    //Improvement GRPCONLEND-18125
    public static void updateNewCollateralFlagToY(Set<Id> creditAppIdSet){
        List<LLC_BI__Product_Package__c> caList =  new List<LLC_BI__Product_Package__c>();
        for (LLC_BI__Product_Package__c singleCA :[select id,CA_with_Land_Collateral__c
                                                   from LLC_BI__Product_Package__c
                                                   where Id=:creditAppIdSet]){
                                                       if(singleCA.CA_with_Land_Collateral__c == False){
                                                           singleCA.CA_with_Land_Collateral__c = True;
                                                           caList.add(singleCA);
                                                       }
                                                   }
        try{
            if(!caList.isEmpty()){
                Database.update(caList);
            }
        }
        catch(Exception e){
            ATH_LogHandler.logException(e, 'CG_CreditAppTriggerHandler', 'updateNewCollateralFlag', 'CreditApplication');
        }
    }
    
    
    public static void getCaIdSetFromListToUpdateNewCollateralFlag(List<LLC_BI__Product_package__c> newCAList){
        Set<Id> caIdSet =new Set<Id>();
        for(LLC_BI__Product_package__c creditApp : newCAList){
            caIdSet.add(creditApp.id);
        }
        updateNewCollateralLandFlagInCA(caIdSet);
    }
    
    
    public static void updateNewCollateralLandFlagInCA(Set<Id> creditAppIdSet){
       
        Map<Id,LLC_BI__Product_Package__c> finalCAMap=new Map<Id,LLC_BI__Product_Package__c>();
        List<LLC_BI__Product_Package__c> caWithAccountAndSolicitorDetails = [select id,LLC_BI__Account__c,CA_with_Land_Collateral__c,CG_AD_Organisation_Name__c,CG_SD_Organisation_Name__c,CG_AD_Contact_Name__c,CG_SD_Contact_Name__c,
                                                                             CG_AD_Street__c,CG_SD_Street__c,CG_AD_Flat__c,CG_SD_Flat__c,CG_AD_Town__c,CG_SD_Town__c,CG_AD_District__c,
                                                                             CG_SD_District__c,CG_AD_House_Name__c,CG_SD_House_Name__c,CG_AD_House_Number__c,CG_SD_House_Number__c,CG_AD_Country__c,
                                                                             CG_SD_Country__c,CG_AD_Post_Code__c,CG_SD_Post_Code__c,CG_AD_Telephone_Number__c,CG_SD_Telephone_Number__c,
                                                                             CG_AD_Telephone_Type__c,CG_SD_Telephone_Type__c,CG_SD_DX_Number__c
                                                                             from LLC_BI__Product_Package__c 
                                                                             where Id=:creditAppIdSet];
        
        List<New_Collateral__c> newCollaterals = [select id,Name, Collateral_Type_NC__c,
                                                  Collateral_Subtype_NC__c,Collateral_Status__c,Credit_Application_NC__c
                                                  from New_Collateral__c where Credit_Application_NC__c=:creditAppIdSet];
        
        
        
        Boolean landExistingCol =false;
        Boolean landExistingAssociations = false;
        Set<Id> getAccountIdToGetOwnershipRecord = new Set<Id>();
        Set<Id> collateralIds = new Set<Id>();
        //get all collateral from org
        for(LLC_BI__Product_package__c singleCreditApp : caWithAccountAndSolicitorDetails){
            getAccountIdToGetOwnershipRecord.add(singleCreditApp.LLC_BI__Account__c);
        }
        
        List<LLC_BI__Account_Collateral__c> colOwnership = [select id, LLC_BI__Collateral__c,LLC_BI__Collateral__r.Name, LLC_BI__Collateral__r.Proposed_Collateral_status__c, LLC_BI__Collateral__r.Collateral_Status__c,llc_bi__collateral__r.LLC_BI__Collateral_Type__r.LLC_BI__Type__c 
                                                            from LLC_BI__Account_Collateral__c 
                                                            where LLC_BI__Account__c =:getAccountIdToGetOwnershipRecord];
        
      
        
        if(!colOwnership.isEmpty()) {
            for(LLC_BI__Account_Collateral__c ac : colOwnership){
                if(ac.llc_bi__collateral__r.LLC_BI__Collateral_Type__r.LLC_BI__Type__c == CG_Constants.COLLATERAL_TYPE_LAND && LandExistingCol ==false && ac.LLC_BI__Collateral__r.Proposed_Collateral_status__c != CG_Constants.PROPOSED_COLLATERAL_STATUS_IGNORE && ac.LLC_BI__Collateral__r.Collateral_Status__c !=CG_Constants.COLLATERAL_STATUS_DISCHARGE_COMPLETED && ac.LLC_BI__Collateral__r.Collateral_Status__c !=CG_Constants.COLLATERAL_STATUS_CHARGE_ABANDONED){
                    landExistingCol = true;
                }
                collateralIds.add(ac.LLC_BI__Collateral__c);
            }
            if(!collateralIds.isEmpty()){
                Set<id> collateralMgmtId=new Set<id>();
                List<Guarantee_Assets__c> assExistingCols = [select id,Type__c,Collateral_Mgmt__c,New_Assets__c,Collateral_Status__c, Collateral_Mgmt__r.Proposed_Collateral_status__c from Guarantee_Assets__c where Asset__c=:collateralIds];
                if(assExistingCols.size()>0 && landExistingCol == false){
                    for(Guarantee_Assets__c gc : assExistingCols){
                        collateralMgmtId.add(gc.Collateral_Mgmt__c);
                    }
                    Map<Id,LLC_BI__Collateral__c> assExistingColMap=new Map<Id,LLC_BI__Collateral__c>();
                    if(!collateralMgmtId.isEmpty()){                    
                        assExistingColMap=new Map<Id,LLC_BI__Collateral__c>([Select Id,LLC_BI__Collateral_Type__c, Collateral_Status__c, Proposed_Collateral_status__c,LLC_BI__Collateral_Type__r.LLC_BI__Type__c from LLC_BI__Collateral__c where id=:collateralMgmtId]);
                    }
                    for(Guarantee_Assets__c gc : assExistingCols){
                        if(gc.Collateral_Mgmt__c!=null && assExistingColMap.get(gc.Collateral_Mgmt__c).LLC_BI__Collateral_Type__r.LLC_BI__Type__c==CG_Constants.COLLATERAL_TYPE_LAND && assExistingColMap.get(gc.Collateral_Mgmt__c).Proposed_Collateral_status__c !=CG_Constants.PROPOSED_COLLATERAL_STATUS_IGNORE && assExistingColMap.get(gc.Collateral_Mgmt__c).Collateral_Status__c !=CG_Constants.COLLATERAL_STATUS_CHARGE_ABANDONED && assExistingColMap.get(gc.Collateral_Mgmt__c).Collateral_Status__c !=CG_Constants.COLLATERAL_STATUS_DISCHARGE_COMPLETED){
                            landExistingAssociations=true;
                        }
                        if(gc.New_Assets__c != null && gc.Type__c == CG_Constants.COLLATERAL_TYPE_LAND && gc.Collateral_Status__c != CG_Constants.PROPOSED_COLLATERAL_STATUS_IGNORE){
                            landExistingAssociations=true;
                        }
                    }
                }
            }
        }
        
        
        Set<Id> getNewCollateralAssociation = new Set<Id>();
        Map<Id, Boolean> caNewLandChecker = new Map<ID, Boolean>();
        Map<Id, Boolean> caNewLandAssociationChecker = new Map<ID, Boolean>();
        
        for(LLC_BI__Product_package__c pp : caWithAccountAndSolicitorDetails){
            caNewLandChecker.put(pp.id, false);
            caNewLandAssociationChecker.put(pp.id, false); 
        }
      
        if(newCollaterals.size() > 0 && landExistingCol == false && landExistingAssociations == false){
            
            for(New_Collateral__c nc : newCollaterals){
                if(nc.Collateral_Type_NC__c == CG_Constants.COLLATERAL_TYPE_LAND && nc.Collateral_Status__c !=CG_Constants.PROPOSED_COLLATERAL_STATUS_IGNORE){
                    caNewLandChecker.put(nc.Credit_Application_NC__c, True);
               
                } 
                getNewCollateralAssociation.add(nc.Id);
            }
            
            set<Id> collateralMgmtGAId=new Set<id>();
            List<Guarantee_Asset_2__c> newColAssociation = [Select id,Collateral_Mgmt_GA__c,New_Collateral_Asset__c, Collateral_Status__c, Type__c, New_Collateral__r.Credit_Application_NC__c from Guarantee_Asset_2__c where New_Collateral__c =:getNewCollateralAssociation];
            if(!newColAssociation.isEmpty()){
                for(Guarantee_Asset_2__c newColAsso : newColAssociation){
                    if(newColAsso!=null && newColAsso.New_Collateral_Asset__c!=null && newColAsso.Type__c == CG_Constants.COLLATERAL_TYPE_LAND && newColAsso.Collateral_Status__c != CG_Constants.PROPOSED_COLLATERAL_STATUS_IGNORE){
                        caNewLandAssociationChecker.put(newColAsso.New_Collateral__r.Credit_Application_NC__c, True);
                       
                    } 
                    if(newColAsso.Collateral_Mgmt_GA__c != null ){
                        collateralMgmtGAId.add(newColAsso.Collateral_Mgmt_GA__c);
                    }
                }
                Map<Id,LLC_BI__Collateral__c> assExistingColMap=New Map<Id,LLC_BI__Collateral__c>([Select Id,LLC_BI__Collateral_Type__c, Collateral_Status__c, Proposed_Collateral_status__c from LLC_BI__Collateral__c where id=:collateralMgmtGAId]);
                for(Guarantee_Asset_2__c newColAsso1 : newColAssociation){
                    if(newColAsso1.Type__c==CG_Constants.COLLATERAL_TYPE_LAND && newColAsso1.Collateral_Mgmt_GA__c != null && assExistingColMap.get(newColAsso1.Collateral_Mgmt_GA__c).Proposed_Collateral_status__c !=  CG_Constants.PROPOSED_COLLATERAL_STATUS_IGNORE && assExistingColMap.get(newColAsso1.Collateral_Mgmt_GA__c).Collateral_Status__c != CG_Constants.COLLATERAL_STATUS_DISCHARGE_COMPLETED && assExistingColMap.get(newColAsso1.Collateral_Mgmt_GA__c).Collateral_Status__c !=CG_Constants.COLLATERAL_STATUS_CHARGE_ABANDONED){
                        
                        caNewLandAssociationChecker.put(newColAsso1.New_Collateral__r.Credit_Application_NC__c, True);
                    } 
                }
            }
        }
        
        
        
        
        LLC_BI__Product_Package__c newpp = new LLC_BI__Product_Package__c();
        for (LLC_BI__Product_Package__c singleCA : caWithAccountAndSolicitorDetails){
            if(landExistingAssociations == true || landExistingCol == true){
                singleCA.CA_with_Land_Collateral__c = True;
                finalCAMap.put(singleCA.Id,singleCA);
            } 
            
            //If organisation does not contain land collateral or land associations for existingcollaterals
            if(landExistingAssociations == false && landExistingCol == false){
                
                //If new collateral in credit application contains land collateral update flag in that CA to true else false
                if(caNewLandChecker.get(singleCA.id) == True || caNewLandAssociationChecker.get(singleCA.id) == True ){
                    singleCA.CA_with_Land_Collateral__c = True;
                    finalCAMap.put(singleCA.Id,singleCA);
                } 
                else if(caNewLandChecker.get(singleCA.id) == False && caNewLandAssociationChecker.get(singleCA.id) == False ){
                    singleCA.CA_with_Land_Collateral__c = False;
                    finalCAMap.put(singleCA.Id,singleCA);
                } 
                
            }
            
            if(caNewLandChecker.get(singleCA.Id) == false && caNewLandAssociationChecker.get(singleCA.Id) == false && landExistingAssociations == false && landExistingCol == false && singleCA.CG_AD_Organisation_Name__c != null && singleCA.CG_AD_Town__c != null && singleCA.CG_AD_Telephone_Number__c != null){
                singleCA.CG_AD_Organisation_Name__c = newpp.CG_AD_Organisation_Name__c;
                singleCA.CG_AD_Contact_Name__c = newpp.CG_AD_Contact_Name__c;
                singleCA.CG_AD_Country__c = newpp.CG_AD_Country__c;
                singleCA.CG_AD_District__c = newpp.CG_AD_District__c;
                singleCA.CG_AD_Flat__c = newpp.CG_AD_Flat__c;
                singleCA.CG_AD_House_Name__c = newpp.CG_AD_House_Name__c;
                singleCA.CG_AD_House_Number__c = newpp.CG_AD_House_Number__c;
                singleCA.CG_AD_Post_Code__c = newpp.CG_AD_Post_Code__c;
                singleCA.CG_AD_Telephone_Number__c = newpp.CG_AD_Telephone_Number__c;
                singleCA.CG_AD_Town__c = newpp.CG_AD_Town__c;
                singleCA.CG_AD_Telephone_Type__c = newpp.CG_AD_Telephone_Type__c;
                singleCA.CG_AD_Street__c = newpp.CG_AD_Street__c;
                finalCAMap.put(singleCA.Id,singleCA);
            }
            if(caNewLandChecker.get(singleCA.Id) == false && caNewLandAssociationChecker.get(singleCA.Id) == false && landExistingAssociations == false && landExistingCol == false && singleCA.CG_SD_Organisation_Name__c != null && singleCA.CG_SD_Town__c != null && singleCA.CG_SD_Telephone_Number__c != null){
                singleCA.CG_SD_Organisation_Name__c = newpp.CG_SD_Organisation_Name__c;
                singleCA.CG_SD_Contact_Name__c = newpp.CG_SD_Contact_Name__c;
                singleCA.CG_SD_Country__c = newpp.CG_SD_Country__c;
                singleCA.CG_SD_District__c = newpp.CG_SD_District__c;
                singleCA.CG_SD_Flat__c = newpp.CG_SD_Flat__c;
                singleCA.CG_SD_House_Name__c = newpp.CG_SD_House_Name__c;
                singleCA.CG_SD_House_Number__c = newpp.CG_SD_House_Number__c;
                singleCA.CG_SD_Post_Code__c = newpp.CG_SD_Post_Code__c;
                singleCA.CG_SD_Telephone_Number__c = newpp.CG_SD_Telephone_Number__c;
                singleCA.CG_SD_Town__c = newpp.CG_SD_Town__c;
                singleCA.CG_SD_Telephone_Type__c = newpp.CG_SD_Telephone_Type__c;
                singleCA.CG_SD_Street__c = newpp.CG_SD_Street__c;
                singleCA.CG_SD_DX_Number__c = newpp.CG_SD_DX_Number__c;
                finalCAMap.put(singleCA.Id,singleCA);
            }
        }
        
        try{
            if(!finalCAMap.isEmpty()){
                Database.update(finalCAMap.values());
            }
        }
        catch(Exception e){
            ATH_LogHandler.logException(e, 'CG_CreditAppTriggerHandler', 'updateNewCollateralFlag', 'CreditApplication');
        }
    }
    
    /**
*  Description    This method updates all facilities SFG flag for  CA
*  @name          updateLoanSFGForCA   
*  @param 		  Set<Id> creditAppIdSet
*  @return 		  Void
*  @throws exception NA
*/    
    public static void updateLoanSFGForCA(Set<Id> creditAppIdSet){
        Set<Id> setOfCAToGetLoan = new Set<Id> ();
        Map<Id, List<LLC_BI__Loan__c>> mapOfCALoan = new Map<Id, List<LLC_BI__Loan__c>>();
        List<LLC_BI__Loan__c> loanList = new List<LLC_BI__Loan__c>();
        Set<Id> setOfCASFGChangedFalse = new Set<Id> ();
        Set<Id> setOfCASFGChangedTrue = new Set<Id> ();
        
        
        for (LLC_BI__Product_Package__c singleCA :[select id,LLC_BI__Account__c,Application_Purpose__c,LLC_BI__Account__r.Company_Registration_Number__c,LLC_BI__Account__r.BTA_Code__c,LLC_BI__Account__r.Type,
                                                   Secure_By_Sfg_Flag__c,isFacilitySFG__c,
                                                   (select id,Name,Collateral_Type_NC__c,Collateral_Subtype_NC__c
                                                    from New_Collaterals__r where Collateral_Subtype_NC__c='Short Form Guarantee')
                                                   from LLC_BI__Product_Package__c
                                                   where Id=:creditAppIdSet and Application_Entity__c='BUK']){
                                                       
                                                       if(singleCA.Application_Purpose__c!= null&&(singleCA.LLC_BI__Account__r.Type=='L'||singleCA.LLC_BI__Account__r.Company_Registration_Number__c!=null ||
                                                                                                   (singleCA.LLC_BI__Account__r.BTA_Code__c!=null&&
                                                                                                    CG_Constants.cilsFullCABTAList.contains(singleCA.LLC_BI__Account__r.BTA_Code__c)))){ 
                                                                                                        
                                                                                                        if(CG_Constants.sfgApplicationPurposeList.contains(singleCA.Application_Purpose__c)){
                                                                                                            setOfCAToGetLoan.add(singleCA.Id);
                                                                                                            if(singleCA.New_Collaterals__r==null||singleCA.New_Collaterals__r.isEmpty()){
                                                                                                                setOfCASFGChangedFalse.add(singleCA.Id);
                                                                                                            }
                                                                                                            else if(singleCA.New_Collaterals__r!=null||!singleCA.New_Collaterals__r.isEmpty()){
                                                                                                                setOfCASFGChangedTrue.add(singleCA.Id);
                                                                                                            }
                                                                                                        }
                                                                                                        else if(singleCA.Application_Purpose__c.equals(CG_Constants.BPAL_APP_PURPOSE)&&(singleCA.LLC_BI__Account__r.BTA_Code__c!=CG_Constants.BTA_CODE)){ 
                                                                                                            setOfCAToGetLoan.add(singleCA.Id);
                                                                                                            if(singleCA.Secure_By_Sfg_Flag__c==false){
                                                                                                                setOfCASFGChangedFalse.add(singleCA.Id);
                                                                                                            } 
                                                                                                            else if(singleCA.Secure_By_Sfg_Flag__c==true){
                                                                                                                setOfCASFGChangedTrue.add(singleCA.Id);
                                                                                                            } 
                                                                                                        }
                                                                                                    }
                                                   }//end of first for loop
       
        
        if(!setOfCAToGetLoan.isEmpty()){
            mapOfCALoan = CG_CATriggerProvider.getLoanRecs(setOfCAToGetLoan);
        }
        
        for(Id key : mapOfCALoan.keySet()){
            for(LLC_BI__Loan__c singleLoan : mapOfCALoan.get(key)){
                if(setOfCASFGChangedFalse.contains(singleLoan.LLC_BI__Product_Package__c)){
                    singleLoan.SFG_Required__c = false;
                }
                else if(setOfCASFGChangedTrue.contains(singleLoan.LLC_BI__Product_Package__c)){
                    singleLoan.SFG_Required__c = true;
                }
                loanList.add(singleLoan); 
            }
        }
      
        try{
            if(!loanList.isEmpty()){
                Database.update(loanList);
            }
        }
        catch(Exception e){
            ATH_LogHandler.logException(e, 'CG_CreditAppTriggerHandler', 'updateLoanSFGForCA', 'CreditApplication');
        }
    }
    
    /**
*  Description    This method dissociate route records from Credit Application
*  @name          dissociateRouteRecordsFromCreditApplication   
*  @param 		  List<LLC_BI__Product_Package__c> creditAppList,Map<Id,LLC_BI__Product_Package__c> oldCAMap
*  @return 		  Void
*  @throws exception NA
*/    
    public static void dissociateRouteRecordsFromCreditApplication(List<LLC_BI__Product_Package__c> creditAppList,Map<Id,LLC_BI__Product_Package__c> oldCAMap){
        final String pendingSTR = 'Pending';
        Set<Id> setOfCreditApplicationIdForNewCollateral = new Set<Id>();
        Set<String> setOfAppPurposesNewCollaterals = new Set<String>(Label.appPurposeNewCollaterals.split(','));
        Set<Id> setOfCreditApplicationIdForQIandILC = new Set<Id>();
        Set<String> setOfAppPurposesQI = new Set<String>(Label.appPurposeQI.split(','));
        Set<String> setOfAppPurposesILC = new Set<String>(Label.appPurposeILC.split(','));
        Set<Id> setOfCreditApplicationIdsForScripts = new Set<Id>();
        Set<String> setOfAppPurposesScripts = new Set<String>(Label.appPuporseScripts.split(','));
        Map<Id,String> mapOfCreditAppIdToTypeOfQI  = new Map<Id,String>();
        Map<Id,String> mapOfCreditAppIdToTypeOfILC  = new Map<Id,String>();
        List<sObject> listOfDissociationRecordsToBeUpdated = new List<sObject>();
        for(LLC_BI__Product_Package__c creditApp : creditAppList){
            /*When the Application Purpose (Application_Purpose__c) field on Credit Application (LLC_BI__product_package__c) gets updated to
(BPAL,Renewal,Existing Customer - Modifications Self Sanction,Cheque Negotiation) then New Collateral(New_Collateral__c) record/records 
should be dissociated from Credit Application. I.e.{'2A','3A','7A','10A'}*/
            if(String.isNotBlank(creditApp.Status__c) &&
               pendingSTR.equalsIgnoreCase(creditApp.Status__c) &&
               creditApp.Application_Purpose__c !=(oldCAMap.get(creditApp.Id)).Application_Purpose__c &&
               setOfAppPurposesNewCollaterals.contains(creditApp.Application_Purpose__c)){
                   setOfCreditApplicationIdForNewCollateral.add(creditApp.Id);
               }
            /*When the Application Purpose (Application_Purpose__c) field on Credit Application (LLC_BI__product_package__c) gets updated to
(Renewal,Change in Legal Status,Existing Customer - Modifications Credit Referral,
Existing Customer - Modifications Self Sanction,Collateral Modification,Cheque Negotiation) then ATHN_CG_Questionnaire__c
record/records should be dissociated from Credit Application.I.e.{'3A','5A','6A','7A','8A','10A'}*/
            if(String.isNotBlank(creditApp.Status__c) &&
               pendingSTR.equalsIgnoreCase(creditApp.Status__c) &&
               creditApp.Application_Purpose__c !=(oldCAMap.get(creditApp.Id)).Application_Purpose__c &&
               setOfAppPurposesQI.contains(creditApp.Application_Purpose__c)){
                   setOfCreditApplicationIdForQIandILC.add(creditApp.Id);
                   mapOfCreditAppIdToTypeOfQI.put(creditApp.Id,'QI');
               }
            /*When the Application Purpose (Application_Purpose__c) field on Credit Application (LLC_BI__product_package__c) gets updated to
(BPAL,Renewal,Change in Legal Status,Existing Customer - Modifications Credit Referral,
Existing Customer - Modifications Self Sanction,Collateral Modification,Cheque Negotiation) then ATHN_CG_Questionnaire__c
record/records should be dissociated from Credit Application.I.e. {'2A','3A','5A','6A','7A','8A','10A'}*/
            if(String.isNotBlank(creditApp.Status__c) &&
               pendingSTR.equalsIgnoreCase(creditApp.Status__c) &&
               creditApp.Application_Purpose__c !=(oldCAMap.get(creditApp.Id)).Application_Purpose__c &&
               setOfAppPurposesILC.contains(creditApp.Application_Purpose__c)){
                   setOfCreditApplicationIdForQIandILC.add(creditApp.Id);
                   mapOfCreditAppIdToTypeOfILC.put(creditApp.Id,'ILC');
               }
            /*When the Application Purpose (Application_Purpose__c) field on Credit Application (LLC_BI__product_package__c) gets updated to
(Cheque Negotiation) then CG_CAConnection__c record/records should be dissociated from Credit Application.I.e.{'10A'}*/
            if(String.isNotBlank(creditApp.Status__c) &&
               pendingSTR.equalsIgnoreCase(creditApp.Status__c) &&
               creditApp.Application_Purpose__c !=(oldCAMap.get(creditApp.Id)).Application_Purpose__c &&
               setOfAppPurposesScripts.contains(creditApp.Application_Purpose__c)){
                   setOfCreditApplicationIdsForScripts.add(creditApp.Id);
               }
        }
        
        if(!setOfCreditApplicationIdForNewCollateral.isEmpty()){
            List<sObject> newCollateralRecordstobeUpdated = CG_CreditAppTriggerHandlerHelper.dissociateNewCollateralRecordsFromCreditApplication(setOfCreditApplicationIdForNewCollateral);
            if(NULL != newCollateralRecordstobeUpdated && !newCollateralRecordstobeUpdated.isEmpty()){
                listOfDissociationRecordsToBeUpdated.addAll(newCollateralRecordstobeUpdated);        
            }
        }
        if(!setOfCreditApplicationIdForQIandILC.isEmpty()){
            List<sObject> questionnaireRecordstobeUpdated = CG_CreditAppTriggerHandlerHelper.dissociateQuestionnaireRecordsFromCreditApplication(mapOfCreditAppIdToTypeOfQI,mapOfCreditAppIdToTypeOfILC,setOfCreditApplicationIdForQIandILC);
            if(NULL != questionnaireRecordstobeUpdated && !questionnaireRecordstobeUpdated.isEmpty()){
                listOfDissociationRecordsToBeUpdated.addAll(questionnaireRecordstobeUpdated);        
            }
        }
        if(!setOfCreditApplicationIdsForScripts.isEmpty()){
            List<sObject> connectionRecordstobeUpdated = CG_CreditAppTriggerHandlerHelper.dissociateScriptsFromCreditApplication(setOfCreditApplicationIdsForScripts);
            if(NULL != connectionRecordstobeUpdated && !connectionRecordstobeUpdated.isEmpty()){
                listOfDissociationRecordsToBeUpdated.addAll(connectionRecordstobeUpdated);        
            }
        }
        try{
            if(NULL!= listOfDissociationRecordsToBeUpdated && !listOfDissociationRecordsToBeUpdated.isEmpty()){
                Database.update(listOfDissociationRecordsToBeUpdated,FALSE);
            }
        }catch(exception e){
            System.debug('Exception-'+e);
            ATH_LogHandler.logException(e, 'CG_CreditAppTriggerHandler', 'dissociateRouteRecordsFromCreditApplication', 'CreditApplication');
        }
    }
    
    /**
*  Description    This method will be called by Trigger before update
*  @name          clearOutBrokerDetailsFromCreditApp   
*  @param 		  List<LLC_BI__Product_Package__c> creditAppList,Map<Id,LLC_BI__Product_Package__c> oldCAMap
*  @return 		  Void
*  @throws exception NA
*/ 
    public static void clearOutBrokerDetailsFromCreditApp(List<LLC_BI__Product_Package__c> creditAppList,Map<Id,LLC_BI__Product_Package__c> oldCAMap){
        final String noSTR = 'No'; 
        final String pendingSTR = 'Pending';
        try{
            for (LLC_BI__Product_Package__c creditAppRecord : creditAppList){
                
                // If Broker Introduced is updated with 'No' value.
                if(pendingSTR.equalsIgnoreCase(creditAppRecord.Status__c) &&
                   NULL != creditAppRecord.Broker_Introduced__c &&
                   creditAppRecord.Broker_Introduced__c != oldCAMap.get(creditAppRecord.Id).Broker_Introduced__c &&
                   noSTR.equalsIgnoreCase(creditAppRecord.Broker_Introduced__c)){
                       //Broker Details
                       //Broker_Details - Fieldset
                       creditAppRecord.CG_BD_Organisation_Name__c = NULL;
                       creditAppRecord.CG_BD_Contact_Name__c = NULL;
                       creditAppRecord.CG_BD_Reference_Number__c = NULL;
                       creditAppRecord.CG_BD_Deal_Number__c = NULL;
                       
                       //Broker_Address - Fieldset
                       creditAppRecord.CG_BD_Flat__c = NULL;
                       creditAppRecord.CG_BD_House_Name__c = NULL;
                       creditAppRecord.CG_BD_House_Number__c = NULL;
                       creditAppRecord.CG_BD_Street__c = NULL;
                       creditAppRecord.CG_BD_District__c = NULL;
                       creditAppRecord.CG_BD_Town__c = NULL;
                       creditAppRecord.CG_BD_Country__c = NULL;
                       creditAppRecord.CG_BD_Post_Code__c = NULL;
                       
                       //Broker_Telephone - Fieldset
                       creditAppRecord.CG_BD_Telephone_Number__c = NULL;
                       creditAppRecord.CG_BD_Telephone_Type__c = NULL;
                   }
            }
        }catch(exception e){
            System.debug('Exception-'+e);
            ATH_LogHandler.logException(e, 'CG_CreditAppTriggerHandler', 'clearOutBrokerDetailsFromCreditApp', 'CreditApplication');
        }
    }
    /**
*  Description    This method will be called by Trigger before update
*  @name          updateRetailWholesaleMarker   
*  @param 		  List<LLC_BI__Product_Package__c> creditAppList,Map<Id,LLC_BI__Product_Package__c> oldCAMap
*  @return 		  Void
*  @throws exception NA
*/ 
            //BUKBBSF-30023 - Shruti Tiwari - START
    public static void updateRetailWholesaleMarker(List<LLC_BI__Product_Package__c> creditAppList,Map<Id,LLC_BI__Product_Package__c> oldCAMap){
        for(LLC_BI__Product_Package__c creditApp:creditAppList){
                                                         
            if(creditApp.Application_Purpose__c!=(oldCAMap.get(creditApp.Id)).Application_Purpose__c)
                                                    {
                                                      if((String.ValueOf(creditApp.Application_Purpose__c)=='1A') || (String.ValueOf(creditApp.Application_Purpose__c)=='9A'))
                                                      {  
                                                          creditApp.RWM_Golden_Source__c='R';
                                                          
                                                     }
                                                        else {
                 
                                                          creditApp.RWM_Golden_Source__c= null;
                                                      } 
                                                  }
        }
    } //BUKBBSF-30023 - Shruti Tiwari - END  
}