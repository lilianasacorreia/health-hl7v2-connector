package utils.schemas

  /**
   * Enumeration representing the HL7 v2 Relationship Code System.
   * These codes identify the relationship between a patient and another person
   * (e.g., contact, family member, etc.) according to the HL7 v2 standard.
   *
   * Commonly used in segments such as NK1 (Next of Kin) to specify the relationship type.
   * SEL - Self: The person is the patient themselves.
   * MTH - Mother: The person is the patient's mother.
   * FTH - Father: The person is the patient's father.
   * SPO - Spouse: The person is the patient's husband, wife, or partner.
   * EXF - Familiar: The person is a family member other than mother, father, or spouse.
   * EMC - Emergency Contact: The person is designated as an emergency contact.
   * OTH - Other: The relationship type does not fit in the predefined categories.
   */
enum RelationshipCodeSystemV2(val code: String):
  case SEL extends RelationshipCodeSystemV2("SEL")
  case MTH extends RelationshipCodeSystemV2("MTH")
  case FTH extends RelationshipCodeSystemV2("FTH")
  case SPO extends RelationshipCodeSystemV2("SPO")
  case EXF extends RelationshipCodeSystemV2("EXF")
  case EMC extends RelationshipCodeSystemV2("EMC")
  case OTH extends RelationshipCodeSystemV2("OTH")
end RelationshipCodeSystemV2
