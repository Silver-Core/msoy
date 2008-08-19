//
// $Id: GameGenrePanel.java 9605 2008-06-27 21:08:39Z sarah $

package client.games;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ChangeListener;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.EnterClickAdapter;
import com.threerings.gwt.ui.InlineLabel;
import com.threerings.msoy.game.gwt.GameInfo;
import com.threerings.msoy.item.data.all.Game;

import client.shell.Args;
import client.shell.DynamicMessages;
import client.shell.Pages;
import client.ui.MsoyUI;
import client.util.Link;

/**
 * Displays the title of the page, "find a game fast" and "search for games" boxes
 */
public class GameHeaderPanel extends FlowPanel
{
    public GameHeaderPanel (
        final byte genre, final byte sortMethod, final String query, String titleText)
    {
        setStyleName("gameHeaderPanel");
        _genre = genre;

        add(MsoyUI.createLabel(titleText, "GenreTitle"));

        // find a game fast dropdown box
        FlowPanel findGame = MsoyUI.createFlowPanel("FindGame");
        findGame.add(MsoyUI.createLabel(_msgs.genreFindGame(), "Title"));
        add(findGame);
        _findGameBox = new ListBox();
        _findGameBox.addItem("", "");
        _findGameBox.addChangeListener(new ChangeListener() {
            public void onChange (Widget widget) {
                ListBox listBox = (ListBox) widget;
                String selectedValue = listBox.getValue(listBox.getSelectedIndex());
                if (!selectedValue.equals("")) {
                    Link.go(Pages.GAMES, Args.compose(new String[] {"d", selectedValue}));
                }
            }
        });
        findGame.add(_findGameBox);

        // search for games
        FlowPanel search = MsoyUI.createFlowPanel("Search");
        search.add(MsoyUI.createLabel(_msgs.genreSearch(), "Title"));
        add(search);
        final TextBox searchBox = new TextBox();
        searchBox.setVisibleLength(20);
        if (query != null) {
            searchBox.setText(query);
        }
        ClickListener searchListener = new ClickListener() {
            public void onClick (Widget sender) {
                String newQuery = searchBox.getText().trim();
                Link.go(Pages.GAMES, Args.compose(
                    new String[] {"g", genre+"", sortMethod+"", newQuery}));
            }
        };
        searchBox.addKeyboardListener(new EnterClickAdapter(searchListener));
        search.add(searchBox);
        Button searchGo = new Button("", searchListener);
        searchGo.setStyleName("GoButton");
        search.add(MsoyUI.createImageButton("GoButton", searchListener));
    }

    /**
     * After data is received, display the genre header and the data grid
     */
    protected void init (List<GameInfo> games)
    {
        // make a copy of the list of games sorted by name for the dropdown
        List<GameInfo> gamesByName = new ArrayList<GameInfo>();
        gamesByName.addAll(games);
        Collections.sort(gamesByName, SORT_GAMEINFO_BY_NAME);
        for (GameInfo gameInfo : gamesByName) {
            String gameName = gameInfo.name;
            _findGameBox.addItem(gameName, gameInfo.gameId+"");
        }

        // add a link to the genre links
        FlowPanel genreLinks = MsoyUI.createFlowPanel("GenreLinks");
        add(genreLinks);
        for (int i = 0; i < Game.GENRES.length; i++) {
            byte genreCode = Game.GENRES[i];
            genreLinks.add(Link.create(_dmsgs.getString("genre" + genreCode),
                Pages.GAMES, Args.compose("g", genreCode)));
            if (i+1 < Game.GENRES.length) {
                genreLinks.add(new InlineLabel("|"));
            }
        }
    }

    /** Compartor for sorting {@link GameInfo}, by name. */
    protected static Comparator<GameInfo> SORT_GAMEINFO_BY_NAME = new Comparator<GameInfo>() {
        public int compare (GameInfo info1, GameInfo info2) {
            return info1.name.toString().toLowerCase().compareTo(
                info2.name.toString().toLowerCase());
        }
    };

    /** Dropdown of all games */
    protected ListBox _findGameBox;

    /** Genre ID or -1 for All Games page */
    protected byte _genre;

    protected static final GamesMessages _msgs = GWT.create(GamesMessages.class);
    protected static final DynamicMessages _dmsgs = GWT.create(DynamicMessages.class);
}
