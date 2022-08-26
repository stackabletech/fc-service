$(document).ready(function() {
      console.log("Loading data");

      //initialise the datatable
      var partDataTable = $('#participantTable').DataTable({
            ajax: {
                url: 'parts',
                dataSrc: 'items'
             },
            order: [[1, 'asc']],
            columns: [
                   {
                            class: 'details-control',
                            orderable: false,
                            data: null,
                            defaultContent: '',
                  },
                  { data: 'id' } ,
                  { data: 'name' },
                  { data: 'publicKey' },
                  { data: 'selfDescription' ,'visible' : false},
                  {
                       orderable: false,
                      "render": function (data) {
                        return '<div><button type="button" class="btn btn-success" id="editButton">Edit</button></div>'
                     }
                  },
                  {
                      orderable: false,
                      "render": function (data) {
                        return '<div><button type="button" class="btn btn-danger" id="deleteButton">Delete</button></div>'
                        }
                  }

                   ]

            } );


            // Array to track the ids of the details displayed rows
                var detailRows = [];

                $('#participantTable tbody').on('click', 'tr td.details-control', function () {
                    var tr = $(this).closest('tr');
                    var row = partDataTable.row(tr);
                    var idx = detailRows.indexOf(tr.attr('id'));

                    if (row.child.isShown()) {
                        tr.removeClass('details');
                        row.child.hide();

                        // Remove from the 'open' array
                        detailRows.splice(idx, 1);
                    } else {
                        tr.addClass('details');
                        row.child(format(row.data())).show();

                        // Add to the 'open' array
                        if (idx === -1) {
                            detailRows.push(tr.attr('id'));
                        }
                    }
                });

                // On each draw, loop over the `detailRows` array and show any child rows
                partDataTable.on('draw', function () {
                    detailRows.forEach(function(id, i) {
                        $('#' + id + ' td.details-control').trigger('click');
                    });
                });

            //Edit button show div
            $('#participantTable').on('click', '#editButton',function(e){
                 e.preventDefault();
                 var data = partDataTable.row( $(this).parents('tr')).data();
                //console.log("edit call"+JSON.stringify(data));
                var sdData=JSON.parse(data['selfDescription']);
                $("#editData").val(JSON.stringify(sdData,undefined, 4));
//                $("#editData").val(data['self-description']);
                $('#editModal').modal('show');
            });

            //Delete  button confirm and call to server for deletion
            $('#participantTable').on('click', '#deleteButton',function(e){
                 e.preventDefault();

                 var check = confirm("Are you sure you want to delete?");
                         if (check == true) {
                             //console.log("Confirm delete");
                             var data = partDataTable.row( $(this).parents('tr')).data();
                             console.log("delete call"+JSON.stringify(data));

                                 $.ajax('/parts/'+data.id, {
                                    type: 'DELETE',  // http method
                                    contentType: "application/json;charset=utf-8",
                                    success: function (data, status, xhr) {
                                        console.log("post delete success");
                                        partDataTable.ajax.reload();

                                    },
                                    error: function (jqXhr, textStatus, errorMessage) {
                                        console.log("post delete failure"+jqXhr);
                                         alert(JSON.stringify(jqXhr));

                                    }
                                });
                         }
                         else {
                          console.log("Cancel delete");
                             return ;
                         }



             });

            //Edit  button  call to server for savings
            $('#submitEditButton').on('click', function(e){

                 var normalData=$("#editData").val();
                 var data=JSON.parse(normalData);
                 //console.log("data"+data);
                 console.log("data::"+data['id']);
                     $.ajax('/parts/'+data['id'], {
                            type: 'PUT',  // http method
                            contentType: "application/json;charset=utf-8",
                            data: JSON.stringify(data),  // data to submit
                            success: function (data, status, xhr) {
                             console.log("post edit success");
                              $('#editModal').modal('hide');
                              partDataTable.ajax.reload();
                            },
                            error: function (jqXhr, textStatus, errorMessage) {
                             console.log("post edit failure:"+jqXhr+"textStatus:"+textStatus+"errorMessage : "+errorMessage);
                             alert(JSON.stringify(jqXhr));
                            }
                     });
             });

            //Add new participants
            $('#submitAddButton').on('click', function(e){

                 var normalData=$("#addData").val();
                 var data=JSON.parse(normalData);
                 //console.log("data"+data);

                     $.ajax('/parts', {
                            type: 'POST',  // http method
                            contentType: "application/json;charset=utf-8",
                            data: JSON.stringify(data),  // data to submit
                            success: function (data, status, xhr) {
                             console.log("post edit success");
                              $('#addModal').modal('hide');
                              partDataTable.ajax.reload();
                            },
                            error: function (jqXhr, textStatus, errorMessage) {
                             console.log("post Add failure:"+jqXhr+"textStatus:"+textStatus+"errorMessage : "+errorMessage);
                              alert(JSON.stringify(jqXhr));
                            }
                     });
             });

            //Button click event call
            $('#addButton').on('click', function(e){
                e.preventDefault();
                $('#addModal').modal('show');
             });

            $('#addButton').on('click', function(e){
                 e.preventDefault();
                console.log("Add call");
            });

            $('.close').on('click', function(e){
                $('#editModal').modal('hide');
                $('#addModal').modal('hide');
            });

 });


 function format(d) {
 //console.log(d);
     return (
         'Self-Description: ' +
         d['selfDescription']
     );
 }