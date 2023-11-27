package eu.xfsc.fc.core.dao.catalogue.node;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.neo4j.core.schema.GeneratedValue;

import java.util.Date;
import java.util.UUID;

@Getter
@Setter
public abstract class BaseEntity {
    @Id
    @GeneratedValue
    private UUID id;
    private String name;
    private Date createdAt = new Date();
}
