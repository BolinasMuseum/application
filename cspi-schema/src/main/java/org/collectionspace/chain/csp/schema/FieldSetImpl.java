package org.collectionspace.chain.csp.schema;

import org.collectionspace.chain.csp.config.ReadOnlySection;


public abstract class FieldSetImpl implements FieldSet {
	
	protected SchemaUtils utils = new SchemaUtils();
	
	protected void initialiseVariables(ReadOnlySection section, String tempid) {
		utils.initBoolean(section,"@services-type-anonymous", true); // generate an embedded anonymous type instead of a standalone complex type
		if (section != null) {
			String servicesType = (String)section.getValue("/@services-type");
			if (servicesType != null && servicesType.isEmpty() == false) {
				this.setServicesType(servicesType);
			}
			String servicesGroupType = (String)section.getValue("/@services-group-type");
			if (servicesGroupType != null && servicesGroupType.isEmpty() == false) {
				this.setServicesGroupType(servicesGroupType);
			}			
		}
		
	}
	
	@Override
	public Boolean isInServices() {
		return utils.getBoolean("@exists-in-services");
	}
	
	@Override
	public Boolean shouldIndex() {
		return utils.getBoolean("@services-should-index");
	}
	
	@Override
	public Boolean isServicesAnonymousType() {
		return utils.getBoolean("@services-type-anonymous");
	}
	
	@Override
	public Boolean isAGroupField() {
		return this.getUIType().startsWith("groupfield");
	}
	
	@Override
	public Boolean isAStructureDate() {
		return this.getUIType().startsWith("groupfield") && this.getUIType().contains("structureddate");
	}	
		
	@Override
	public String getID() {
		return  utils.getString("@id");
	}
	
	@Override
	public String getParentID() {
		return utils.getString("parentID");
	}
	
	private String getServicesType(String attributeName, boolean namespaceQualified) {
		String result = utils.getString(attributeName);
		String nsPrefix = NS;

		if (result == null) {
			if (this.isAGroupField() == true) {
				result = this.getUIType().split("/")[1]; // returns "bar" from something like "foo/bar"
			} else {
				String datatype = utils.getString("@datatype");
				if (datatype != null && !datatype.isEmpty()) {
					nsPrefix = XS;
					if (datatype.equalsIgnoreCase(DATATYPE_FLOAT)) {
						datatype = DATATYPE_DECIMAL;
					} else if (datatype.equalsIgnoreCase(DATATYPE_LARGETEXT)) {
						datatype = DATATYPE_STRING;
					}
					result = datatype;
				}
			}
		}
		
		if (result != null && namespaceQualified == true) {
			result = nsPrefix + result;
		}
		
		return result;
	}
	
	@Override
	public String getServicesGroupType(boolean namespaceQualified) {
		return getServicesType("services-group-type", namespaceQualified);
	}
	
	@Override
	public void setServicesGroupType(String servicesGroupType) {
		utils.setString("services-group-type", servicesGroupType);
	}
		
	@Override
	public String getServicesType(boolean namespaceQualified) {
		return getServicesType("services-type", namespaceQualified);
	}
	
	@Override
	public String getServicesType() {
		return getServicesType(true /* default to getting namespace qualified */);
	}
	
	@Override
	public void setServicesType(String servicesType) {
		utils.setString("services-type", servicesType);
	}
	
}