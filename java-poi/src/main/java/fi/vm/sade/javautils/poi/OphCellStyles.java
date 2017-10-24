package fi.vm.sade.javautils.poi;

import org.apache.commons.lang3.StringUtils;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.util.function.Consumer;

public abstract class OphCellStyles<S extends CellStyle, C extends Cell> {
    private final S quotePrefixStyle;
    private final S unsafeStyle;

    protected OphCellStyles(S quotePrefixStyle, S unsafeStyle) {
        this.quotePrefixStyle = quotePrefixStyle;
        quotePrefixStyle.setQuotePrefixed(true);
        this.unsafeStyle = unsafeStyle;
    }

    public C apply(C cell) {
        String value = cell.getStringCellValue();
        if (StringUtils.startsWithAny(value, "=", "@", "-", "+")) {
            cell.setCellStyle(quotePrefixStyle);
        } else {
            cell.setCellStyle(unsafeStyle);
        }
        return cell;
    }

    public void visit(Consumer<S> visitor) {
        visitor.accept(quotePrefixStyle);
        visitor.accept(unsafeStyle);
    }

    public static class OphHssfCellStyles extends OphCellStyles<HSSFCellStyle, HSSFCell> {
        public OphHssfCellStyles(HSSFWorkbook workbook) {
            super(workbook.createCellStyle(), workbook.createCellStyle());
        }
    }

    public static class OphXssfCellStyles extends OphCellStyles<XSSFCellStyle, XSSFCell> {
        public OphXssfCellStyles(XSSFWorkbook workbook) {
            super(workbook.createCellStyle(), workbook.createCellStyle());
        }
    }
}
