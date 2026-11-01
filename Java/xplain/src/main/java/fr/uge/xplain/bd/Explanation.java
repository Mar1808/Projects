package fr.uge.xplain.bd;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import org.hibernate.type.descriptor.jdbc.TimestampUtcAsJdbcTimestampJdbcType;

import java.util.Objects;

@Entity
public class Explanation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    private String javaClass;
    private String error;
    private String xplanation;
    private String correction;
    public Explanation() {

    }
    public Explanation(String javaClass, String error, String xplanation, String correction) {
        this.javaClass = Objects.requireNonNull(javaClass);
        this.error = Objects.requireNonNull(error);
        this.xplanation = Objects.requireNonNull(xplanation);
        this.correction = Objects.requireNonNull(correction);
    }
    Long getId(){
        return id;
    }

    @Override
    public String toString() {
        return javaClass + " " + error + " " + xplanation + " " + correction;
    }

    public String getJavaClass() {
        return javaClass;
    }
    public String getError() {
        return error;
    }
    public String getXplanation() {
        return xplanation;
    }
    public String getCorrection() {return correction;}
}
