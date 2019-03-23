package com.patatos.sac.berkensap2;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Random;

import com.patatos.sac.blamathview.BlaMathView;

public class MainActivity extends AppCompatActivity {

    private static final String TAGS_FILE = "tags_file";

    private static final String SPLIT_MARK = "&qu";
    private static final String EQ_MARK = "&eq";
    private static final String SHOW_MARK = "&sh";
    private static final String TAG_MARK = "&st";
    private static final String END_MARK = "&n";

    private static final String W_START = "$$";
    private static final String W_END = "$$";

    private static final String HEAD = "<script type=\"text/javascript\">function e(value){var a=\"\";if(document.getElementById(value).style.visibility=='visible'){a='hidden';}else{a='visible';}document.getElementById(value).style.visibility=a}</script>";
    private static String POOP(String sh, String n) { return "<div class=\"container\"><table class=\"spoiler\"onclick=\"javascript:e('s"+n+"')\"><tbody><td><b class=\"c\">"+sh+":</b><br><br><div class=\"b\"id=\"s"+n+"\"style=\"visibility:hidden\">"; }
    private static final String BUTT = "<br><br><br></div></td></tbody></table></div>";

    private Random rand;

    private BlaMathView formula;
    private EditText input;

    private String tex = "$$\\sum_{k=0}^n k^2 = \\frac{(n^2+n)(2n+1)}{6}$$";
    private int texID = -1;
    private int texNB = 0;

    private String[] tags;
    private String[] tagsOnly;
    private int[] tagsMap;
    private boolean newTagsAdded = false;
    private ArrayList<Integer> missingTags = new ArrayList<>();

    private boolean isLoaded = false;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.activity_main);

        this.rand = new Random();

        if (!this.isLoaded) {
            SharedPreferences pref = this.getSharedPreferences("preferences", Context.MODE_PRIVATE);
            this.texNB = pref.getInt("nb", 0);
            String missingTagsStr = pref.getString("missingTags", "");
            Log.i("onCreate", this.texNB + "");

            if (this.texNB == 0) {
                try {
                    this.openFileOutput(TAGS_FILE, Context.MODE_APPEND).close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (!missingTagsStr.equals("") && !missingTagsStr.equals(","))
                for (String it : missingTagsStr.split(","))
                    if (!it.equals(""))
                        this.missingTags.add(Integer.valueOf(it));

            this.tags = this.readPrivateStrings(TAGS_FILE, TAG_MARK);
            this.makeTagsOnly();
            this.makeTagsMap();

            this.isLoaded = true;
        }

        this.formula = (BlaMathView) this.findViewById(R.id.formula);
        this.input = (EditText) this.findViewById(R.id.input);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        this.getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public void onResume() {
        this.formula = (BlaMathView) this.findViewById(R.id.formula);
        this.input = (EditText) this.findViewById(R.id.input);

        super.onResume();
    }

    @Override
    public void onStop() {
        String missingTagsStr = "";
        for (Integer it : this.missingTags)
            missingTagsStr+= it+",";

        SharedPreferences.Editor prefEditor = this.getSharedPreferences("preferences", Context.MODE_PRIVATE).edit();
        prefEditor.putInt("nb", this.texNB);
        prefEditor.putString("missingTags", missingTagsStr);
        prefEditor.apply();

        if (this.newTagsAdded) {
            this.deleteFile(TAGS_FILE);
            ArrayList<String> toSort = new ArrayList<>();
            for (String it : this.tags)
                if (it.contains(":")) {
                    int i = Integer.valueOf(it.split(":")[0]);

                    boolean good = true;
                    for (int j : this.missingTags)
                        if (i == j) {
                            good = false;
                            break;
                        }

                    if (good)
                        toSort.add(it);
                }
            String[] toSortArray = new String[toSort.size()];
            for (int i = 0; i < toSortArray.length; i++)
                toSortArray[i] = toSort.get(i);

            Arrays.sort(toSortArray, new Comparator<String>() {
                @Override
                public int compare(String o1, String o2) {
                    if (o1.contains(":") && o2.contains(":"))
                        return o1.split(":", 2)[1].toLowerCase().charAt(0) - o2.split(":", 2)[1].toLowerCase().charAt(0);
                    return 0;
                }
            });
            try {
                FileOutputStream fos = openFileOutput(TAGS_FILE, Context.MODE_APPEND);
                for (String it : toSortArray)
                    fos.write((it + TAG_MARK).getBytes());
                fos.flush();
                fos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        super.onStop();
    }

    public void onActionShow    () {
        this.setTex(this.input.getText().toString());
    }
    public void onActionSave    () {
        this.tex = this.input.getText().toString();

        final ArrayList<Integer> selectedTagsID = new ArrayList<>();

        String[] fullTagList = new String[this.tagsOnly.length+1];
        fullTagList[0] = "new tag";
        System.arraycopy(this.tagsOnly, 0, fullTagList, 1, this.tagsOnly.length);

        AlertDialog.Builder builder1 = new AlertDialog.Builder(this);
        builder1.setMultiChoiceItems(fullTagList, null,
                new DialogInterface.OnMultiChoiceClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                        if (which == 0)
                            createNewTag(new Runnable() {
                                @Override
                                public void run() {
                                    selectedTagsID.add(tags.length-2);
                                }
                            });
                        else
                            selectedTagsID.add(Integer.valueOf(tags[which-1].split(":")[0]));
                    }

                });
        builder1.setPositiveButton("Save", new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (!tex.equals("")) {
                    if (selectedTagsID.size() != 0) {
                        String texTags = "";
                        for (int it : selectedTagsID)
                            texTags += it + ",";

                        tex = texTags.substring(0, texTags.length()-1) + TAG_MARK + tex;
                    }

                    texID = texNB;
                    texNB++;
                    saveTex(tex);
                    setTex(tex);
                    input.setText("");
                }
            }

        });
        builder1.show();
    }
    public void onActionClear   () {
        for (int i = 0; i < this.texNB; i++)
            this.deleteFile(i + "");
        this.deleteFile(TAGS_FILE);
    }
    public void onActionSac     () {
        String s = "";

        String tmp = "";
        for (Integer it : this.missingTags)
            tmp+= it+",";

        s+= "File content:";
        s+= "\n\t" + this.texNB + " element" + (this.texNB > 1 ? "s" : "") + "\nmissingTags:" + tmp + "\n\n";

        if (this.tags != null) {
            s+= "Tags saved:";
            for (String it : this.tags)
                s+= "\n\t" + it;
        }

        AlertDialog.Builder b = new AlertDialog.Builder(this);
        b.setPositiveButton("Load file", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                onActionLoad();
            }
        });
        b.setNeutralButton("Save file", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                onActionFile();
            }
        });
        b.setNegativeButton("Edit a tag", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                onActionEdiTag();
            }
        });
        b.setMessage(s).show();
    }
    public void onActionLoad    () {
        final MainActivity that = this;
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        final LinearLayout layout = new LinearLayout(this);
        final EditText inputTex = new EditText(this);
        final EditText inputTag = new EditText(this);

        layout.setOrientation(LinearLayout.VERTICAL);
        inputTex.setInputType(InputType.TYPE_CLASS_TEXT);
        inputTag.setInputType(InputType.TYPE_CLASS_TEXT);
        inputTex.setHint("Tex file");
        inputTag.setHint("Tag file");
        layout.addView(inputTex);
        layout.addView(inputTag);

        builder.setTitle("Enter files names");
        builder.setView(layout);

        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (inputTex.getText().toString().equals("") && inputTag.getText().toString().equals("")) {
                    Toast.makeText(that, "Empty inputs!", Toast.LENGTH_SHORT).show();
                    return;
                }

                int b;
                String strTex = "";
                String strTag = "";

                FileInputStream fis;
                FileOutputStream fos;
                BufferedInputStream bis;
                BufferedOutputStream bos;

                if (!inputTex.getText().toString().equals("")) {
                    try {
                        fis = new FileInputStream(inputTex.getText().toString());
                        bis = new BufferedInputStream(fis);
                        while ((b = bis.read()) != -1)
                            strTex+= (char) b;
                        bis.close();
                        fis.close();

                        String[] file = strTex.split(END_MARK);
                        for (int i = 0; i < file.length; i++) {
                            FileOutputStream f = openFileOutput(i + "", Context.MODE_PRIVATE);
                            f.write(file[i].getBytes());
                            f.flush();
                            f.close();
                        }
                        texNB = file.length;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                if (!inputTag.getText().toString().equals("")) {
                    try {
                        fis = new FileInputStream(inputTag.getText().toString());
                        fos = openFileOutput(TAGS_FILE, Context.MODE_APPEND);
                        bis = new BufferedInputStream(fis);
                        bos = new BufferedOutputStream(fos);
                        while ((b = bis.read()) != -1) {
                            bos.write(b);
                            strTag+= (char) b;
                        }
                        bis.close();
                        bos.close();
                        fis.close();
                        fos.close();

                        tags = strTag.split(TAG_MARK);
                        newTagsAdded = true;
                        makeTagsOnly();
                        makeTagsMap();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
    }
    public void onActionReplace () {
        this.tex = this.input.getText().toString();

        if (this.tex.equals("")) {
            this.saveTex(this.readPrivateString(this.texNB + ""));
            this.setTex("\\text{Removed}");
            this.input.setText("");

            this.deleteFile(this.texNB + "");
            this.texNB--;
        } else {
            this.saveTex(this.tex);
            this.setTex(this.tex);
            this.input.setText("");
        }
    }
    public void onActionEdit    () {
        this.input.setText(this.readPrivateString(this.texID + ""));
    }
    public void onActionFile    () {
        final MainActivity that = this;

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        final EditText input = new EditText(this);

        input.setInputType(InputType.TYPE_CLASS_TEXT);

        builder.setTitle("Enter directory (end with '/')");
        builder.setView(input);

        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                try {
                    BufferedOutputStream bos1 = new BufferedOutputStream(new FileOutputStream(input.getText().toString() + "out_texs.txt"));
                    for (int i = 0; i < texNB; i++)
                        bos1.write((readPrivateString(i + "") + END_MARK + "\n").getBytes());
                    bos1.flush();
                    bos1.close();

                    BufferedOutputStream bos2 = new BufferedOutputStream(new FileOutputStream(input.getText().toString() + "out_tags.txt"));
                    for (String c : tags)
                        bos2.write((c + TAG_MARK + "\n").getBytes());
                    bos2.flush();
                    bos2.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                Toast.makeText(that, "File saved", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
    }
    public void onActionEditTags() {
        this.tex = this.readPrivateString(this.texID + "");

        final ArrayList<Integer> selectedTagsID2 = new ArrayList<>();

        String[] fullTagList2 = new String[this.tagsOnly.length+1];
        fullTagList2[0] = "new tag";
        System.arraycopy(this.tagsOnly, 0, fullTagList2, 1, this.tagsOnly.length);

        boolean[] checked = new boolean[this.tags.length+1];

        if (this.tex.contains(TAG_MARK))
            for (String c : this.tex.split(TAG_MARK)[0].split(",")) {
                checked[this.tagsMap[Integer.parseInt(c)] + 1] = true;
                selectedTagsID2.add(Integer.parseInt(c));
            }

        AlertDialog.Builder builder2 = new AlertDialog.Builder(this);
        builder2.setMultiChoiceItems(fullTagList2, checked,
                new DialogInterface.OnMultiChoiceClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                        if (which == 0)
                            createNewTag(new Runnable() {
                                @Override
                                public void run() {
                                    selectedTagsID2.add(tags.length-2);
                                }
                            });
                        else {
                            if (isChecked)
                                selectedTagsID2.add(Integer.valueOf(tags[which - 1].split(":")[0]));
                            else
                                selectedTagsID2.remove(Integer.valueOf(tags[which - 1].split(":")[0]));
                        }
                    }

                });
        builder2.setPositiveButton("Save", new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                String tmp;
                if (tex.contains(TAG_MARK))
                    tmp = tex.split(TAG_MARK)[1];
                else
                    tmp = tex;

                if (selectedTagsID2.size() != 0) {
                    String texTags = "";
                    for (int it : selectedTagsID2)
                        texTags += it + ",";

                    tmp = texTags.substring(0, texTags.length()-1) + TAG_MARK + tmp;
                }

                tex = tmp;
                saveTex(tmp);
                setTex(tmp);
            }

        });
        builder2.show();
    }
    public void onActionEdiTag  () {
        final MainActivity that = this;

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMultiChoiceItems(this.tags, new boolean[this.tags.length],
                new DialogInterface.OnMultiChoiceClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, final int which, boolean isChecked) {
                        AlertDialog.Builder builder_ = new AlertDialog.Builder(that);
                        final EditText input_ = new EditText(that);

                        input_.setInputType(InputType.TYPE_CLASS_TEXT);
                        input_.setText(tags[which]);

                        builder_.setTitle("Edit tag");
                        builder_.setView(input_);

                        builder_.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog_, int which_) {
                                String newTag = input_.getText().toString();
                                if (newTag.equals(""))
                                    missingTags.add(Integer.valueOf(tags[which].split(":")[0]));

                                newTagsAdded = true;
                                tags[which] = newTag;
                                makeTagsOnly();
                                makeTagsMap();
                            }
                        });
                        builder_.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog_, int which_) {
                                dialog_.cancel();
                            }
                        });

                        builder_.show();
                    }

                });
        builder.setNegativeButton("Back", new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }

        });
        builder.show();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

            case R.id.action_show    : this.onActionShow    (); break;
            case R.id.action_save    : this.onActionSave    (); break;
            case R.id.action_clear   : this.onActionClear   (); break;
            case R.id.action_sac     : this.onActionSac     (); break;
            case R.id.action_load    : this.onActionLoad    (); break;
            case R.id.action_replace : this.onActionReplace (); break;
            case R.id.action_edit    : this.onActionEdit    (); break;
            case R.id.action_file    : this.onActionFile    (); break;
            case R.id.action_editTags: this.onActionEditTags(); break;
        }

        return super.onOptionsItemSelected(item);
    }

    public void makeTagsOnly() {
        this.tagsOnly = new String[this.tags.length];

        for (int i = 0; i < this.tagsOnly.length; i++) {
            if (this.tags[i].contains(":"))
                this.tagsOnly[i] = this.tags[i].split(":")[1];
            else
                this.tagsOnly[i] = "";
        }
    }
    public void makeTagsMap() {
        this.tagsMap = new int[this.tags.length];

        for (int i = 0; i < this.tagsMap.length; i++)
            if (this.tags[i].contains(":"))
                this.tagsMap[Integer.valueOf(this.tags[i].split(":")[0])] = i;
    }

    public void createNewTag(final Runnable onNewTagAdded) {
        final EditText inp = new EditText(this);
        inp.setInputType(InputType.TYPE_CLASS_TEXT);

        AlertDialog.Builder builder2 = new AlertDialog.Builder(this);
        builder2.setTitle("Enter tag value");
        builder2.setView(inp);
        builder2.setPositiveButton("Save", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                try {
                    int newTagN = (missingTags.size() > 0)? missingTags.remove(0) : (tags.length-1);
                    FileOutputStream fos = openFileOutput(TAGS_FILE, Context.MODE_APPEND);
                    fos.write((newTagN + ":" + inp.getText().toString() + TAG_MARK).getBytes());
                    fos.flush();
                    fos.close();

                    tags = readPrivateStrings(TAGS_FILE, TAG_MARK);
                    makeTagsOnly();
                    makeTagsMap();
                    newTagsAdded = true;

                    onNewTagAdded.run();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        builder2.show();
    }

    public void buttonRandom(View view) {
        if (this.texNB < 0)
            return;

        this.texID = this.rand.nextInt(this.texNB);
        this.tex = this.readPrivateString(this.texID + "");

        this.setTex(this.tex);
    }

    private ArrayList<String> found;
    private ArrayList<Integer> foundID;
    public void buttonSearch(View view) {
        this.found = new ArrayList<>();
        this.foundID = new ArrayList<>();
        final ArrayList<Integer> selectedTagsID = new ArrayList<>();

        AlertDialog.Builder builder1 = new AlertDialog.Builder(this);
        builder1.setMultiChoiceItems(/*Arrays.copyOfRange(*/this.tagsOnly/*, 0, this.tagsOnly.length-1)*/, new boolean[this.tags.length],
                new DialogInterface.OnMultiChoiceClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                        //selectedTagsID.add(which);
                        selectedTagsID.add(Integer.valueOf(tags[which].split(":")[0]));
                    }

                });
        builder1.setPositiveButton("Search", new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                /*for (int i = 0; i < selectedTagsID.size(); i++)
                    selectedTagsID.set(i, Integer.valueOf(tags[selectedTagsID.get(i)].split(":")[0]));*/
                processSearch(selectedTagsID);
            }

        });
        builder1.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }

        });
        builder1.show();
    }
    public void processSearch(ArrayList<Integer> selectedTagsID) {
        String srchstr = this.input.getText().toString().toLowerCase().replaceAll("[\\s\\{\\}]", "");

        for (int i = 0; i < this.texNB; i++) {
            String[] t = this.readPrivateStringUntil(i + "", '&').split(",");
            boolean keep = true;

            for (int tagID : selectedTagsID) {
                boolean check = false;

                for (String it : t)
                    if (it.equals(tagID + "")) {
                        check = true;
                        break;
                    }

                if (!check) {
                    keep = false;
                    break;
                }
            }

            if (!keep)
                continue;

            boolean contains = true;
            String c = this.readPrivateString(i + "");
            if (!this.input.getText().toString().equals(""))
                contains = c.toLowerCase().replaceAll("[\\s\\{\\}]", "").contains(srchstr);
            if (contains) {
                this.found.add(c);
                this.foundID.add(i);
            }
        }

        if (this.found.size() == 1) {
            this.texID = this.foundID.get(0);
            this.setTex(this.found.get(0));
        }
        else if (this.foundID.size() == 0) {
            this.texID = -1;
            this.setTex("\\text{No result found}");
            Log.i("search", "No result found");
        } else {
            String[] foundDesc = new String[this.foundID.size()];
            for (int i = 0; i < this.foundID.size(); i++) {
                String it = this.found.get(i);
                if (it.contains(TAG_MARK))
                    it = it.split(TAG_MARK)[1];
                foundDesc[i] = it.contains(SPLIT_MARK)? it.split(SPLIT_MARK)[1] : it.split(EQ_MARK)[0];
            }

            AlertDialog.Builder builder2 = new AlertDialog.Builder(this);
            builder2.setMultiChoiceItems(foundDesc, new boolean[foundID.size()],
                    new DialogInterface.OnMultiChoiceClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                            texID = foundID.get(which);
                            setTex(found.get(which));
                            dialog.cancel();
                        }

                    });
            builder2.show();
        }
    }

    protected void setTex(String c) {
        String main = c.split(TAG_MARK)[c.contains(TAG_MARK)? 1:0];

        if (!main.contains(SPLIT_MARK) && !main.contains(EQ_MARK)) {
            this.formula.setText(W_START + main + W_END);
            return;
        }

        String[] tmp = main.split(SPLIT_MARK);
        String[] bla = tmp[0].split(EQ_MARK);

        String sac = HEAD + (tmp.length > 1 ? W_START + tmp[1] + ":" + W_END : "");
        for (int i = 0; i < bla.length; i++)
            sac+= POOP(bla[i].contains(SHOW_MARK)? bla[i].split(SHOW_MARK)[1] : "Unfold",
                    String.valueOf(i)) + W_START + bla[i].split(SHOW_MARK)[0] + W_END + BUTT;

        this.formula.setText(sac);
    }

    protected void saveTex(String c) {
        try {
            FileOutputStream f = this.openFileOutput(this.texID + "", Context.MODE_PRIVATE);
            BufferedOutputStream bos = new BufferedOutputStream(f);

            bos.write(c.getBytes());

            bos.flush();
            bos.close();
            f.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String[] readPrivateStrings(String filename, String eol) {
        String raw = "";
        int buffer;

        try {
            FileInputStream f = this.openFileInput(filename);
            BufferedInputStream bis = new BufferedInputStream(f);
            while ((buffer = bis.read()) != -1)
                raw+= (char) buffer;
            bis.close();
            f.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return raw.split(eol);
    }
    private String readPrivateString(String filename) {
        String raw = "";
        int buffer;

        try {
            FileInputStream f = this.openFileInput(filename);
            BufferedInputStream bis = new BufferedInputStream(f);
            while ((buffer = bis.read()) != -1)
                raw+= (char) buffer;
            bis.close();
            f.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return raw;
    }
    private String readPrivateStringUntil(String filename, char eof) {
        String raw = "";
        int buffer;

        try {
            FileInputStream f = this.openFileInput(filename);
            BufferedInputStream bis = new BufferedInputStream(f);
            while ((buffer = bis.read()) != -1 && !raw.endsWith(eof + ""))
                raw+= (char) buffer;
            bis.close();
            f.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return raw.substring(0, raw.length()-1);
    }

}
