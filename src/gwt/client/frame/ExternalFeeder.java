//
// $Id$

package client.frame;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.user.client.rpc.AsyncCallback;

import com.threerings.gwt.util.ServiceUtil;

import com.threerings.msoy.data.all.DeploymentConfig;

import com.threerings.msoy.web.gwt.FacebookTemplateCard;
import com.threerings.msoy.web.gwt.Pages;
import com.threerings.msoy.web.gwt.WebMemberService;
import com.threerings.msoy.web.gwt.WebMemberServiceAsync;

import client.shell.CShell;
import client.util.InfoCallback;
import client.util.JavaScriptUtil;
import client.util.events.FlashEvents;
import client.util.events.TrophyEvent;

/**
 * Handles publishing events to external feeds like Facebook or OpenSocial.
 */
public class ExternalFeeder
{
    public ExternalFeeder ()
    {
        FlashEvents.addListener(new TrophyEvent.Listener() {
            public void trophyEarned (TrophyEvent event) {
                publishTrophyToFacebook(event);
            }
        });
    }

    protected void publishTrophyToFacebook (final TrophyEvent event)
    {
        _membersvc.getFacebookTemplate("trophy", new InfoCallback<FacebookTemplateCard>() {
            @Override public void onSuccess (FacebookTemplateCard result) {
                if (result != null) {
                    CShell.log("Got template, publishing", "bundle", result.bundleId,
                        "variant", result.variant);
                    publishTrophyToFacebook(event, result);
                } // else oops
            }
        });
    }

    protected void publishTrophyToFacebook (TrophyEvent event, FacebookTemplateCard template)
    {
        String vector = template.toEntryVector("trophy");
        String templateId = String.valueOf(template.bundleId);

        List<Object> images = new ArrayList<Object>();
        images.add(createImage(event.getMediaURL(),
            Pages.GAMES.makeURL("vec", vector, event.getGameId(), "d", "t")));

        if (DeploymentConfig.devDeployment) {
            setPublicImages(images);
        }

        Map<String, Object> data = new HashMap<String, Object>();
        data.put("game_id", event.getGameId());
        data.put("game", event.getGame());
        data.put("game_desc", event.getGameDescription());
        data.put("trophy", event.getTrophy());
        data.put("descrip", event.getDescription());
        data.put("action_url",
            DeploymentConfig.facebookCanvasUrl + "?game=" + event.getGameId() + "&vec=" + vector);
        data.put("vector", vector);
        data.put("images", JavaScriptUtil.createArray(images));

        publishTrophy(templateId, event.getGameId(), event.getTrophyIdent(),
            JavaScriptUtil.createDictionaryFromMap(data));
    }

    /**
     * Creates a JSNI dictionary referring to the given image source and href.
     */
    protected JavaScriptObject createImage (String src, String href)
    {
        Map<String, Object> image = new HashMap<String, Object>();
        image.put("src", src);
        image.put("href", href);
        return JavaScriptUtil.createDictionaryFromMap(image);
    }

    /**
     * Swap in some images with arbitrary public URLs here to satisfy Facebook's validation.
     */
    protected void setPublicImages (List<Object> images)
    {
        for (int ii = 0; ii < images.size(); ++ii) {
            images.set(ii, createImage(
                "http://mediacloud.whirled.com/240aa9267fa6dc8422588e6818862301fd658e6f.png",
                "http://www.whirled.com/go/games-d_827_t"));
        }
    }

    /**
     * Called by facebook.js when the trophy feed publish dialog is closed. There is no guarantee
     * that the user actually chose to do it.
     */
    protected void trophyPublished (int gameId, String trophyIdent)
    {
        _membersvc.trophyPublishedToFacebook(gameId, trophyIdent, new AsyncCallback<Void>() {
            @Override public void onFailure (Throwable caught) {
                CShell.log("Failed to contact server for trophy published", caught);
            }
            @Override public void onSuccess (Void result) {
            }
        });
    }

    protected native void publishTrophy (
        String templateId, int gameId, String ident, JavaScriptObject data) /*-{
        var trophyPublished = this.@client.frame.ExternalFeeder::trophyPublished(ILjava/lang/String;);
        $wnd.FB_PostTrophy(templateId, data, function () {
            trophyPublished(gameId, ident);
        });
    }-*/;


    // Handy JSON for pasting into Facebook's template editor
    /*
      {
          "game_id" : 827,
          "game" : "Corpse Craft",
          "game_desc" :
              "Build an army of corpses to destroy your foes in this puzzle-action hybrid.",
          "trophy" : "Freshman",
          "vector" : "v.none",
          "action_url": "http://www.whirled.com/go/games-d_827",
          "images" : [ {"src" :
              "http://mediacloud.whirled.com/240aa9267fa6dc8422588e6818862301fd658e6f.png",
              "href" : "http://www.whirled.com/go/games-d_827_t"}]}
    */

    protected static final WebMemberServiceAsync _membersvc = (WebMemberServiceAsync)
        ServiceUtil.bind(GWT.create(WebMemberService.class), WebMemberService.ENTRY_POINT);
}
