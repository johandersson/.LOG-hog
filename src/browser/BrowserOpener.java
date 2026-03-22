/*
 * Copyright (C) 2025 Johan Andersson
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package browser;

import java.awt.*;
import java.net.URI;
import gui.DialogHelper;
import security.PathValidator;
import utils.Log;

public class BrowserOpener {
    public static void openInBrowser(String url) {
        try {
            if (!PathValidator.isSafeHttpUrl(url)) {
                DialogHelper.showWarning(null, "Blocked URL", "Blocked URL",
                        "Opening this URL is not allowed. Only HTTP/HTTPS links are supported.");
                return;
            }
            Desktop.getDesktop().browse(new URI(url));
        } catch (Exception e) {
            // Do not expose internal exception details to the user; log safely
            Log.error("Error opening URL in browser.", e);
        }
    }
}
