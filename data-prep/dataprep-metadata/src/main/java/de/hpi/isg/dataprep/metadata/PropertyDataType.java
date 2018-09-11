package de.hpi.isg.dataprep.metadata;

import de.hpi.isg.dataprep.exceptions.DuplicateMetadataException;
import de.hpi.isg.dataprep.exceptions.MetadataNotFoundException;
import de.hpi.isg.dataprep.exceptions.MetadataNotMatchException;
import de.hpi.isg.dataprep.exceptions.RuntimeMetadataException;
import de.hpi.isg.dataprep.model.repository.MetadataRepository;
import de.hpi.isg.dataprep.model.target.objects.Metadata;
import de.hpi.isg.dataprep.model.target.objects.MetadataScope;
import de.hpi.isg.dataprep.util.DataType;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Lan Jiang
 * @since 2018/8/25
 */
public class PropertyDataType extends Metadata {

    private final String name = "property-data-type";

    private DataType.PropertyType propertyDataType;

    public PropertyDataType(MetadataScope scope, DataType.PropertyType propertyDataType) {
        this.scope = scope;
        this.propertyDataType = propertyDataType;
    }

    public MetadataScope getScope() {
        return scope;
    }

    public DataType.PropertyType getPropertyDataType() {
        return propertyDataType;
    }

    @Override
    public String getName() {
        return scope.getName();
    }

    @Override
    public void checkMetadata(MetadataRepository metadataRepository) throws RuntimeMetadataException {
        List<PropertyDataType> matchedInRepo = metadataRepository.getMetadataPool().stream()
                .filter(metadata -> metadata instanceof PropertyDataType)
                .map(metadata -> (PropertyDataType) metadata)
//                .filter(metadata -> metadata.getName().equals(this.scope.getName()))
//                .filter(metadata -> metadata.getPropertyName().equals(this.propertyName))
                .filter(metadata -> metadata.equals(this))
                .collect(Collectors.toList());

        if (matchedInRepo.size() == 0) {
            throw new MetadataNotFoundException(String.format("Metadata %s not found in the repository.", this.toString()));
        } else if (matchedInRepo.size() > 1) {
            throw new DuplicateMetadataException(String.format("Metadata %s has multiple data type for property: %s",
                    this.getClass().getSimpleName(), this.scope.getName()));
        } else {
            PropertyDataType metadataInRepo = matchedInRepo.get(0);
//            if (!this.propertyDataType.equals(metadataInRepo.getPropertyDataType())) {
            if (!this.equalsByValue(metadataInRepo)) {
                // value of this metadata does not match that in the repository.
                throw new MetadataNotMatchException(String.format("Metadata value does not match that in the repository."));
            }
        }
    }

    @Override
    public boolean equalsByValue(Metadata metadata) {
        return this.getPropertyDataType().equals(((PropertyDataType)metadata).getPropertyDataType());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PropertyDataType)) return false;
        PropertyDataType that = (PropertyDataType) o;
        return Objects.equals(scope, that.scope) && Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(scope, name);
    }

    @Override
    public String toString() {
        return "PropertyDataType{" +
                "propertyName='" + scope.getName() + '\'' +
                ", propertyDataType=" + propertyDataType +
                '}';
    }
}