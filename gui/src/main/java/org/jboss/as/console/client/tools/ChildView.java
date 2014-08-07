package org.jboss.as.console.client.tools;

import com.google.gwt.cell.client.ActionCell;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.gwt.view.client.SingleSelectionModel;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.widgets.ContentDescription;
import org.jboss.as.console.client.widgets.tables.ViewLinkCell;
import org.jboss.as.console.mbui.widgets.AddressUtils;
import org.jboss.as.console.mbui.widgets.ModelNodeCellTable;
import org.jboss.as.console.mbui.widgets.ModelNodeColumn;
import org.jboss.as.console.mbui.widgets.ModelNodeForm;
import org.jboss.as.console.mbui.widgets.ModelNodeFormBuilder;
import org.jboss.ballroom.client.rbac.SecurityContext;
import org.jboss.ballroom.client.widgets.forms.FormValidation;
import org.jboss.ballroom.client.widgets.tables.DefaultPager;
import org.jboss.ballroom.client.widgets.tools.ToolButton;
import org.jboss.ballroom.client.widgets.tools.ToolStrip;
import org.jboss.ballroom.client.widgets.window.DefaultWindow;
import org.jboss.ballroom.client.widgets.window.DialogueOptions;
import org.jboss.ballroom.client.widgets.window.Feedback;
import org.jboss.ballroom.client.widgets.window.WindowContentBuilder;
import org.jboss.dmr.client.ModelNode;
import org.jboss.dmr.client.Property;

import java.util.List;

/**
 * @author Heiko Braun
 * @since 24/07/14
 */
public class ChildView {

    private ModelNode currentAddress;
    private boolean currentSquatting;

    private BrowserNavigation presenter;
    private ModelNodeCellTable table;
    private ListDataProvider<ModelNode> dataProvider;
    private SingleSelectionModel<ModelNode> selectionModel;
    private HTML header;
    private ToolStrip tools;

    Widget asWidget() {

        VerticalPanel layout = new VerticalPanel();
        layout.setStyleName("fill-layout-width");

        tools = new ToolStrip();
        tools.addToolButtonRight(new ToolButton("Add", new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                presenter.onPrepareAddChildResource(currentAddress, currentSquatting);
            }
        }));

        final ToolButton remove = new ToolButton("Remove", new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                ModelNode selection = selectionModel.getSelectedObject();
                if (selection != null)
                    presenter.onRemoveChildResource(currentAddress, selection);
            }
        });
        tools.addToolButtonRight(remove);
        remove.setEnabled(false);

        table = new ModelNodeCellTable(12);
        table.addColumn(new ModelNodeColumn(new ModelNodeColumn.ValueAdapter() {
            @Override
            public String getValue(ModelNode model) {
                return model.asString();
            }
        }), "Child Resource" );


        Column<ModelNode, ModelNode> option = new Column<ModelNode, ModelNode>(
                new ViewLinkCell<ModelNode>(Console.CONSTANTS.common_label_view(), new ActionCell.Delegate<ModelNode>() {
                    @Override
                    public void execute(ModelNode selection) {
                        presenter.onViewChild(currentAddress, selection.asString());
                    }
                })
        ) {
            @Override
            public ModelNode getValue(ModelNode model) {
                return model;
            }
        };
        table.addColumn(option, "Option");

        dataProvider = new ListDataProvider<ModelNode>();
        dataProvider.addDataDisplay(table);


        selectionModel = new SingleSelectionModel<ModelNode>();
        table.setSelectionModel(selectionModel);
        selectionModel.addSelectionChangeHandler(new SelectionChangeEvent.Handler() {
            @Override
            public void onSelectionChange(SelectionChangeEvent event) {
                ModelNode selection = selectionModel.getSelectedObject();
                remove.setEnabled(selection!=null);
            }
        });

        DefaultPager pager = new DefaultPager();
        pager.setDisplay(table);

        // -----

        header = new HTML();

        layout.add(header);
        layout.add(tools);
        layout.add(table);
        layout.add(pager);
        return layout;

    }

    public void setPresenter(BrowserNavigation presenter) {
        this.presenter = presenter;
    }

    public void setChildren(ModelNode address, List<ModelNode> modelNodes, boolean flagSquatting) {

        this.currentAddress = address;
        this.currentSquatting = flagSquatting;

        String text = flagSquatting ? "Nested Types" : "Child Resources";
        header.setHTML("<h2 class='homepage-secondary-header'>"+text+" ("+modelNodes.size()+")</h2>");
        dataProvider.setList(modelNodes);

        // squatters cannot be added/removed
        tools.setVisible(!flagSquatting);

    }

    /**
     * Callback for creation of add dialogs.
     * Will be invoked once the presenter has loaded the resource description.
     *  @param address
     * @param securityContext
     * @param description
     */
    public void showAddDialog(final ModelNode address, SecurityContext securityContext, ModelNode description) {

        String resourceAddress = AddressUtils.asKey(address, false);
        if(securityContext.getOperationPriviledge(resourceAddress, "add").isGranted()) {
            _showAddDialog(address, securityContext, description);
        }
        else
        {
            Feedback.alert("Authorisation Required", "You seem to lack permissions to add new resources!");
        }

    }

    private void _showAddDialog(final ModelNode address, SecurityContext securityContext, ModelNode description) {
        List<Property> tuples = address.asPropertyList();
        String type = "";
        if(tuples.size()>0)
        {
            type = tuples.get(tuples.size()-1).getName();
        }

        ModelNodeFormBuilder builder = new ModelNodeFormBuilder()
                .setCreateMode(true)
                .setResourceDescription(description)
                .setSecurityContext(securityContext);

        ModelNodeFormBuilder.FormAssets assets = builder.build();

        final ModelNodeForm form = assets.getForm();
        form.setEnabled(true);

        if(form.hasWritableAttributes()) {
            final DefaultWindow window = new DefaultWindow("Create Resource '" + type + "'");
            window.addStyleName("browser-view");

            DialogueOptions options = new DialogueOptions(new ClickHandler() {
                @Override
                public void onClick(ClickEvent event) {
                    // save
                    FormValidation validation = form.validate();
                    if(!validation.hasErrors())
                    {
                        presenter.onAddChildResource(address, form.getUpdatedEntity());
                        window.hide();
                    }
                }
            }, new ClickHandler() {
                @Override
                public void onClick(ClickEvent event) {
                    // cancel
                    window.hide();
                }
            });

            VerticalPanel layout = new VerticalPanel();
            layout.setStyleName("fill-layout-width");
            ModelNode opDescription = description.get("operations").get("add").get("description");
            ContentDescription text = new ContentDescription(opDescription.asString());
            layout.add(text);
            layout.add(form.asWidget());

            WindowContentBuilder content = new WindowContentBuilder(layout, options);
            window.trapWidget(content.build());
            window.setGlassEnabled(true);
            window.setWidth(480);
            window.setHeight(360);
            window.center();
        }
        else
        {
            // no writable attributes
            Feedback.alert("Cannot create child resource", "There are no configurable attributes on resources " + address);
        }
    }
}