/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     thibaud
 */
package org.nuxeo.diff.pictures;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CloseableFile;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.platform.commandline.executor.api.CmdParameters;
import org.nuxeo.ecm.platform.commandline.executor.api.CommandLineExecutorService;
import org.nuxeo.ecm.platform.commandline.executor.api.CommandNotAvailable;
import org.nuxeo.ecm.platform.commandline.executor.api.ExecResult;
import org.nuxeo.ecm.platform.commandline.executor.service.CommandLineDescriptor;
import org.nuxeo.ecm.platform.commandline.executor.service.CommandLineExecutorComponent;
import org.nuxeo.ecm.platform.web.common.vh.VirtualHostHelper;
import org.nuxeo.runtime.api.Framework;

/**
 * @since 7.4
 */
public class DiffPictures {

    public static final String DEFAULT_COMMAND = "diff-pictures-default";

    public static final String COMPARE_PRO_COMMAND = "diff-pictures-pro";

    public static final String DEFAULT_XPATH = "file:content";

    public static final String DEFAULT_FUZZ = "0";

    public static final String DEFAULT_HIGHLIGHT_COLOR = "Red";

    public static final String DEFAULT_LOWLIGHT_COLOR = "None";

    protected static final String TEMP_DIR_PATH = System.getProperty("java.io.tmpdir");

    // See nuxeo-diff-pictures-template.html
    protected static final String TMPL_PREFIX = "{{";

    protected static final String TMPL_SUFFIX = "}}";

    protected static final String TMPL_CONTEXT_PATH = buildTemplateKey("CONTEXT_PATH");

    protected static final String TMPL_ACTION = buildTemplateKey("ACTION");

    protected static final String TMPL_LEFT_DOC_ID = buildTemplateKey("LEFT_DOC_ID");

    protected static final String TMPL_LEFT_DOC_LABEL = buildTemplateKey("LEFT_DOC_LABEL");

    protected static final String TMPL_RIGHT_DOC_ID = buildTemplateKey("RIGHT_DOC_ID");

    protected static final String TMPL_RIGHT_DOC_LABEL = buildTemplateKey("RIGHT_DOC_LABEL");

    protected static final String TMPL_XPATH = buildTemplateKey("XPATH");

    protected static final String TMPL_TIME_STAMP = buildTemplateKey("TIME_STAMP");

    protected static final String TMPL_HIDE_TUNING = buildTemplateKey("HIDE_TUNING");

    protected static final String TMPL_FORCED_COMMAND = buildTemplateKey("FORCED_COMMAND");

    protected static final String TMPL_HIDE_TOOLS_INLINE_CSS = buildTemplateKey("HIDE_TOOLS_INLINE_CSS");
    
    protected static final String TMPL_IMG_RESULT_NB_COLUMNS = buildTemplateKey("IMG_RESULT_NB_COLUMNS");
    
    protected static final String TMPL_IMG_RESULT_INLINE_CSS = buildTemplateKey("IMG_RESULT_INLINE_CSS");

    Blob b1;

    Blob b2;

    String leftDocId;

    String rightDocId;

    String commandLine;

    Map<String, Serializable> clParameters;

    protected static String buildTemplateKey(String inName) {
        return TMPL_PREFIX + inName + TMPL_SUFFIX;
    }

    public DiffPictures(Blob inB1, Blob inB2) {

        this(inB1, inB2, null, null);

    }

    public DiffPictures(DocumentModel inLeft, DocumentModel inRight) {

        this(inLeft, inRight, null);
    }

    public DiffPictures(DocumentModel inLeft, DocumentModel inRight, String inXPath) {

        Blob leftB, rightB;

        leftB = DiffPicturesUtils.getDocumentBlob(inLeft, inXPath);
        rightB = DiffPicturesUtils.getDocumentBlob(inRight, inXPath);

        init(leftB, rightB, inLeft.getId(), inRight.getId());
    }

    public DiffPictures(Blob inB1, Blob inB2, String inLeftDocId, String inRightDocId) {
        init(inB1, inB2, inLeftDocId, inRightDocId);

    }

    private void init(Blob inB1, Blob inB2, String inLeftDocId, String inRightDocId) {

        b1 = inB1;
        b2 = inB2;
        leftDocId = inLeftDocId;
        rightDocId = inRightDocId;

    }

    public Blob compare(String inCommandLine, Map<String, Serializable> inParams) throws CommandNotAvailable,
            IOException {

        String finalName;

        commandLine = StringUtils.isBlank(inCommandLine) ? DEFAULT_COMMAND : inCommandLine;
        // Being generic, if in the future we add more command lines in the xml.
        // We know that "compare" can't work with pictures of different format or size, so let's force another command
        CommandLineDescriptor cld = CommandLineExecutorComponent.getCommandDescriptor(commandLine);
        if (cld.getCommand().equals("compare") && !DiffPicturesUtils.sameFormatAndDimensions(b1, b2)) {
            commandLine = COMPARE_PRO_COMMAND;
        }

        clParameters = inParams == null ? new HashMap<>() : inParams;

        finalName = (String) clParameters.get("targetFileName");
        if (StringUtils.isBlank(finalName)) {
            finalName = "comp-" + b1.getFilename();
        }

        CloseableFile cf1 = null, cf2 = null;
        String filePath1 = null, filePath2 = null;
        CommandLineExecutorService cles = Framework.getService(CommandLineExecutorService.class);
        CmdParameters params = cles.getDefaultCmdParameters();

        try {
            cf1 = b1.getCloseableFile();
            filePath1 = cf1.getFile().getAbsolutePath();
            params.addNamedParameter("file1", filePath1);

            cf2 = b2.getCloseableFile();
            filePath2 = cf2.getFile().getAbsolutePath();
            params.addNamedParameter("file2", filePath2);

            checkDefaultParametersValues();
            for (Entry<String, Serializable> entry : clParameters.entrySet()) {
                params.addNamedParameter(entry.getKey(), (String) entry.getValue());
            }

            String destFilePath;
            String destFileExtension;

            int dotIndex = finalName.lastIndexOf('.');
            if (dotIndex < 0) {
                destFileExtension = ".tmp";
            } else {
                destFileExtension = finalName.substring(dotIndex);
            }

            Blob tempBlob = Blobs.createBlobWithExtension(destFileExtension);
            destFilePath = tempBlob.getFile().getAbsolutePath();

            params.addNamedParameter("targetFilePath", destFilePath);

            ExecResult execResult = cles.execCommand(commandLine, params);

            // WARNING
            // ImageMagick can return a non zero code with some of its commands,
            // while the execution went totally OK, with no error. The problem is
            // that the CommandLineExecutorService assumes a non-zero return code is
            // an error => we must handle the thing by ourselves, basically just
            // checking if we do have a comparison file created by ImageMagick
            File tempDestFile = tempBlob.getFile();
            if (!tempDestFile.exists() || tempDestFile.length() < 1) {
                throw new NuxeoException("Failed to execute the command <" + commandLine + ">. Final command [ "
                        + execResult.getCommandLine() + " ] returned with error " + execResult.getReturnCode(),
                        execResult.getError());
            }

            tempBlob.setFilename(finalName);

            return tempBlob;

        } catch (IOException e) {
            if (filePath1 == null) {
                throw new IOException("Could not get a valid File from left blob.", e);
            }
            if (filePath2 == null) {
                throw new IOException("Could not get a valid File from right blob.", e);
            }

            throw e;

        } finally {
            if (cf1 != null) {
                cf1.close();
            }
            if (cf2 != null) {
                cf2.close();
            }
        }
    }

    /*
     * Adds the default values if a parameter is missing. This applies for all command lines (and some will be unused)
     */
    protected void checkDefaultParametersValues() {

        if (isDefaultValue((String) clParameters.get("fuzz"))) {
            clParameters.put("fuzz", DEFAULT_FUZZ);
        }

        if (isDefaultValue((String) clParameters.get("highlightColor"))) {
            clParameters.put("highlightColor", DEFAULT_HIGHLIGHT_COLOR);
        }

        if (isDefaultValue((String) clParameters.get("lowlightColor"))) {
            clParameters.put("lowlightColor", DEFAULT_LOWLIGHT_COLOR);
        }

    }

    protected boolean isDefaultValue(String inValue) {
        return StringUtils.isBlank(inValue) || inValue.toLowerCase().equals("default");
    }

    public static String buildDiffHtml(DocumentModel leftDoc, DocumentModel rightDoc, String xpath) throws IOException {
        String html = "";
        InputStream in = null;
        try {
            in = DiffPictures.class.getResourceAsStream("/files/nuxeo-diff-pictures-template.html");
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            String line;
            while ((line = reader.readLine()) != null) {
                html += line + "\n";
            }

        } finally {
            if (in != null) {
                in.close();
            }
        }

        String leftDocId = leftDoc.getId();
        String rightDocId = rightDoc.getId();
        String leftLabel = leftDoc.getTitle();
        String rightLabel = rightDoc.getTitle();
        if (leftLabel.equals(rightLabel)) {
            if (leftDoc.isVersion()) {
                leftLabel = "Version " + leftDoc.getVersionLabel();
            }
            if (rightDoc.isVersion()) {
                rightLabel = "Version " + rightDoc.getVersionLabel();
            }
        }

        // Append info to the labels
        String leftFormat = (String) leftDoc.getPropertyValue("picture:info/format");
        String rightFormat = (String) rightDoc.getPropertyValue("picture:info/format");
        Long leftW = (Long) leftDoc.getPropertyValue("picture:info/width");
        Long rightW = (Long) rightDoc.getPropertyValue("picture:info/width");
        Long leftH = (Long) leftDoc.getPropertyValue("picture:info/height");
        Long rightH = (Long) rightDoc.getPropertyValue("picture:info/height");
        leftLabel += " (" + leftFormat + ", " + leftW + "x" + leftH + ")";
        rightLabel += " (" + rightFormat + ", " + rightW + "x" + rightH + ")";

        // Update UI and command line to use, if needed
        if (leftFormat.toLowerCase().equals(rightFormat.toLowerCase()) && leftW.longValue() == rightW.longValue()
                && leftH.longValue() == rightH.longValue()) {
            html = html.replace(TMPL_HIDE_TUNING, "false");
            html = html.replace(TMPL_HIDE_TOOLS_INLINE_CSS, "");
            html = html.replace(TMPL_IMG_RESULT_NB_COLUMNS, "twelve");
            html = html.replace(TMPL_IMG_RESULT_INLINE_CSS, "");
            html = html.replace(TMPL_FORCED_COMMAND, "");
            
        } else {
            html = html.replace(TMPL_HIDE_TUNING, "true");
            html = html.replace(TMPL_HIDE_TOOLS_INLINE_CSS, "display:none;");
            html = html.replace(TMPL_IMG_RESULT_NB_COLUMNS, "sixteen");
            html = html.replace(TMPL_IMG_RESULT_INLINE_CSS, "padding-left: 35px;");
            html = html.replace(TMPL_FORCED_COMMAND, COMPARE_PRO_COMMAND);
        }

        html = html.replace(TMPL_CONTEXT_PATH, VirtualHostHelper.getContextPathProperty());
        html = html.replace(TMPL_ACTION, "diff");
        html = html.replace(TMPL_LEFT_DOC_ID,
                StringEscapeUtils.escapeJavaScript(StringEscapeUtils.escapeHtml(leftDocId)));
        html = html.replace(TMPL_LEFT_DOC_LABEL,
                StringEscapeUtils.escapeJavaScript(StringEscapeUtils.escapeHtml(leftLabel)));
        html = html.replace(TMPL_RIGHT_DOC_ID,
                StringEscapeUtils.escapeJavaScript(StringEscapeUtils.escapeHtml(rightDocId)));
        html = html.replace(TMPL_RIGHT_DOC_LABEL,
                StringEscapeUtils.escapeJavaScript(StringEscapeUtils.escapeHtml(rightLabel)));
        if (StringUtils.isBlank(xpath) || xpath.toLowerCase().equals("default")) {
            xpath = DEFAULT_XPATH;
        }
        html = html.replace(TMPL_XPATH, StringEscapeUtils.escapeJavaScript(StringEscapeUtils.escapeHtml(xpath)));
        // dc:modified can be null... When running the Unit Tests for example
        String lastModification;
        Calendar cal = (Calendar) rightDoc.getPropertyValue("dc:modified");
        if (cal == null) {
            lastModification = "" + Calendar.getInstance().getTimeInMillis();
        } else {
            lastModification = "" + cal.getTimeInMillis();
        }
        html = html.replace(TMPL_TIME_STAMP, lastModification);

        return html;
    }

}
