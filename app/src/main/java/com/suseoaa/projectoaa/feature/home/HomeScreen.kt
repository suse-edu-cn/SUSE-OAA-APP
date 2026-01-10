package com.suseoaa.projectoaa.feature.home

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.suseoaa.projectoaa.feature.academicPortal.getCourseInfo.GetCourseInfoViewModel
import org.jsoup.Jsoup
import org.jsoup.nodes.Document


val HtmlResponse = """


<style type="text/css">
	.index_title span.title {
  		font: bold 15px/35px "microsoft YaHei";
  		padding-left: 5px;
	}
	.index_title{
		width: 100%;
    	height: 32px;
    	border-bottom: #eeeeee;
	}
	.sl_nav_tabs {
		background: #f4f4f4;
		padding-top: 0px;
		padding-left: 0px;
		
	}
	.index_rctx .list-group-item .title {
		padding-top: 5px;
		margin-left:-12px;
	}
	.badge_xx{
	    position: absolute;
	    top: -2px;
	    right: -3px;
	    background: #f50;
	    font-size: 14px;
	    transform: scale(.7);
	    z-index: 99;
	}
</style>
<input type="hidden" name="jsgs" value="" id="jsgs"/>
<script type="text/javascript">
	//获取更多链接的href,并赋值
	var hrefz = $('#lj').attr("href");
	// if($("#jsgs").val()>1){
	// 	hrefz = document.getElementById("xxyeqian").getElementsByTagName("a")[2].getAttribute('href');
	// }else{
	// 	hrefz = document.getElementById("xxyeqian").getElementsByTagName("a")[1].getAttribute('href');
	// }
	document.getElementById("lj").href = hrefz +1;
	
	$('#xxTab a[data-toggle="tab"]').on('shown.bs.tab', function (e) {
	 	$($(this).attr("href")).find(".chosen-select").trigger("chosen");
	 	if($($(this).attr("href")).selector == '#home' || $($(this).attr("href")).selector == '#ybtips'){
	 		document.getElementById("lj").href = hrefz +1;
	 	}else{
	 		document.getElementById("lj").href = hrefz +2;
	 	}
	 	if($($(this).attr("href")).selector == '#programme'){
	 		$("#dbsygd").hide();
	 	}else{
	 		$("#dbsygd").show();
	 	}
	});
	
	//更新消息状态
	$("#home").off("click","a[name='xxdlTail1']").on("click","a[name='xxdlTail1']",function(){
		$.post(_path + '/xtgl/index_cxXxdlztgx.html',{zjxx:$(this).attr("data-zjxx")},function(data){},'json');
		var tkxx = $(this).attr("data-tkxx"); 
		$.showDialog(_path + "/xtgl/index_cxTtkxx.html",'消息查看',$.extend(true,{},viewConfig,{
			"width":"1000px",
			"data":{"tkxx":tkxx}
		}));
		jbsx();
	});

	$("#ybtips").off("click","a[name='xxdlTail1']").on("click","a[name='xxdlTail1']",function(){
		var tkxx = $(this).attr("data-tkxx");
		$.showDialog(_path + "/xtgl/index_cxTtkxx.html",'消息查看',$.extend(true,{},viewConfig,{
			"width":"1000px",
			"data":{"tkxx":tkxx}
		}));
	});
	
	$("#profile").off("click","a[name='xxdlTail1']").on("click","a[name='xxdlTail1']",function(){
		$.post(_path + '/xtgl/index_cxXxdlztgx.html',{zjxx:$(this).attr("data-zjxx")},function(data){},'json');
		var tkxx = $(this).attr("data-tkxx");
		$.showDialog(_path + "/xtgl/index_cxTtkxx.html",'消息查看',$.extend(true,{},viewConfig,{
			"width":"1000px",
			"data":{"tkxx":tkxx}
		}));
	});
	 
	$("#programme").off("click","a[name='programmeDetail']").on("click","a[name='programmeDetail']",function(){
		var sxdm = $(this).attr("data-sxdm");
		var mxlx = $(this).attr("data-mxlx");
		if(mxlx=='1'){
			var dyym = $(this).attr("data-dyym");
			openWin('..' + dyym, { 
				'gnmkdm': sxdm
			}, true);
		}else{
			$.showDialog(_path + "/xtgl/index_cxProgrammeDetail.html",$("#localChange").val()=='en_US'?'Detail':'日程明细',$.extend(true,{},viewConfig,{
				"width":"1000px",
				"data":{"gnmkdm_ej":sxdm}
			}));
		}
	});
</script>
<input type="hidden" name="messageListSize" value="" id="messageListSize"/>

	<div id="xxyeqian" class="widget-toolbar no-border">
		<ul class="nav nav-tabs sl_nav_tabs" id="xxTab">
			<li class="active">
				<a data-toggle="tab" href="#home">
					<h5 class="index_title"><span class="title">消息</span></h5>
				</a>
			</li>
			
			
			
			
			
				<p align="right" id="dbsygd"  style="padding-top: 15px;font-size:16px;padding-right: 15px;"><a href="/xtgl/index_cxDbsy.html?flag=" target="_blank" id="lj">更多</a></p>
			

		</ul>
	</div>

<div class="tab-content">
	<div id="home" class="tab-pane in active">
		<div class="list-group dtsx">
		
		
			
			
				<a href="#" name="xxdlTail1" data-zjxx="425D7DD345E29651E065000000000001" target="_blank" class="list-group-item" data-tkxx="调课提醒:曾文丽老师于第18周星期四第5-8节在LA6-458上的网络安全综合实践课程调课到由陈继鑫老师在第9周星期一第5-8节LA13-316上课，请各位同学相互告知！" onclick="return false">
					<span class="title" title="调课提醒:曾文丽老师于第18周星期四第5-8节在LA6-458上的网络安全综合实践课程调课到由陈继鑫老师在第9周星期一第5-8节LA13-316上课，请各位同学相互告知！">
					调课提醒:曾文丽老师于第18周星期四第5...
					</span>
					<span class="fraction float_r">
					2025-10-30 17:10:39
					</span>
				</a>
			
			
		
		
			
			
				<a href="#" name="xxdlTail1" data-zjxx="425D7DD345B69651E065000000000001" target="_blank" class="list-group-item" data-tkxx="调课提醒:曾文丽老师于第17周星期四第5-8节在LA6-458上的网络安全综合实践课程调课到由陈继鑫老师在第10周星期一第1-2节,5-6节LA13-316上课，请各位同学相互告知！" onclick="return false">
					<span class="title" title="调课提醒:曾文丽老师于第17周星期四第5-8节在LA6-458上的网络安全综合实践课程调课到由陈继鑫老师在第10周星期一第1-2节,5-6节LA13-316上课，请各位同学相互告知！">
					调课提醒:曾文丽老师于第17周星期四第5...
					</span>
					<span class="fraction float_r">
					2025-10-30 17:09:37
					</span>
				</a>
			
			
		
		
			
			
				<a href="#" name="xxdlTail1" data-zjxx="4221D6B267F4FD83E065000000000001" target="_blank" class="list-group-item" data-tkxx="停课提醒:原定陈超（计算机女）老师在第8-9周星期二第1-2节于LA5-441上的网络互联技术课程停上，请各位同学相互告知！" onclick="return false">
					<span class="title" title="停课提醒:原定陈超（计算机女）老师在第8-9周星期二第1-2节于LA5-441上的网络互联技术课程停上，请各位同学相互告知！">
					停课提醒:原定陈超（计算机女）老师在第8...
					</span>
					<span class="fraction float_r">
					2025-10-27 17:59:50
					</span>
				</a>
			
			
		
		
			
			
				<a href="#" name="xxdlTail1" data-zjxx="4221D6B267B1FD83E065000000000001" target="_blank" class="list-group-item" data-tkxx="停课提醒:原定薛红老师在第8-9周星期二第1-2节于LA5-441上的网络互联技术课程停上，请各位同学相互告知！" onclick="return false">
					<span class="title" title="停课提醒:原定薛红老师在第8-9周星期二第1-2节于LA5-441上的网络互联技术课程停上，请各位同学相互告知！">
					停课提醒:原定薛红老师在第8-9周星期二...
					</span>
					<span class="fraction float_r">
					2025-10-27 17:59:50
					</span>
				</a>
			
			
		
		
			
			
				<a href="#" name="xxdlTail1" data-zjxx="41DE4F4F6AFD53F7E065000000000001" target="_blank" class="list-group-item" data-tkxx="补课提醒:陈年老师将在第9周星期二第5-6节对课程LINUX系统及应用进行补课，请各位同学相互告知！" onclick="return false">
					<span class="title" title="补课提醒:陈年老师将在第9周星期二第5-6节对课程LINUX系统及应用进行补课，请各位同学相互告知！">
					补课提醒:陈年老师将在第9周星期二第5-...
					</span>
					<span class="fraction float_r">
					2025-10-24 09:17:19
					</span>
				</a>
			
			
		
			
		
			
		
			
		
			
		
			
		
			
		
			
		
			
		
			
		
			
		
			
		
			
		
			
		
			
		
			
		
			
		
			
		
			
		
			
		
			
		
			
		
			
		
			
		
			
		
			
		
			
		
			
		
			
		
			
		
			
		
			
		
			
		
			
		
			
		
			
		
			
		
			
		
			
		
			
		
			
		
			
		
			
		
			
		
			
		
			
		
			
		
			
		
			
		
			
		
			
		
			
		
			
		
			
		
			
		
			
		
			
		
			
		
			
		
			
		
			
		
			
		
			
		
			
		
			
		
			
		
			
		
			
		
			
		
			
		
			
		
			
		
			
		
			
		
		
		</div>
	</div>
	<div id="ybtips" class="tab-pane">
		<div class="list-group ">
			
			
		</div>
	</div>
	<div id="profile" class="tab-pane">
		<div style="display: block;">
			<div class="list-group">
				
				
				
				<div ><p style="color:brown;position:absolute;bottom:0px;padding:15px;margin:0px;">提示:此页消息查看需先切换至对应的角色</div>
			</div>
		</div>
	</div>
	
	<div id="programme" class="tab-pane">
		<div style="display: block;">
			<div class="list-group">
				
				
					<a href="#" class="list-group-item">&nbsp;<span class="time float_r">&nbsp;</span></a>
				
					<a href="#" class="list-group-item">&nbsp;<span class="time float_r">&nbsp;</span></a>
				
					<a href="#" class="list-group-item">&nbsp;<span class="time float_r">&nbsp;</span></a>
				
					<a href="#" class="list-group-item">&nbsp;<span class="time float_r">&nbsp;</span></a>
				
					<a href="#" class="list-group-item">&nbsp;<span class="time float_r">&nbsp;</span></a>
				
			</div>
		</div>
	</div>
</div>

<script type="text/javascript">
	$(function (){
		var messageListSize = $("#messageListSize").val();
		if(messageListSize != null && 0 < messageListSize){
			$.confirm("您有未读消息，是否打开消息总览页面？",function(result){
				if(result){
					openWin(_path + '/xtgl/index_cxDbsy.html?flag=1', {}, true);
				}
			});
		}
	});
	/**
	 * 点击后模块自动局部刷新
	 */
	function jbsx(){
		// 局部刷新
		// 加载第三块内容
		$("#area_three").load(_path + "/xtgl/index_cxAreaThree.html?localeKey="+$("#localChange").val(), {}, function() {
			// 滚动条
			if($("#localChange").val()=='en_US'){//切换系统名称为英文的
				$("#lj").text("...More");
			}
		});
	}
</script>
""".trimIndent()
val doc: Document = Jsoup.parse(HtmlResponse)
val MessageInfo = doc.select("div.list-group.dtsx a[data-tkxx]")
    .map { it.attr("data-tkxx") }
@Composable
fun HomeScreen() {
//    LazyColumn(
//        modifier = Modifier
//            .padding(16.dp)
//    ) {
//        items(MessageInfo) {
//            Text(it)
//        }
//    }
    GetCourseInfo()
}

@Composable
fun GetCourseInfo(viewModel: GetCourseInfoViewModel = hiltViewModel()) {
    val courseInfo = viewModel.fetchAcademicCourseInfo().toString().trimIndent()
    val doc: Document = Jsoup.parse(courseInfo)
    val academicCourseChangeInfo = doc.select("div.list-group.dtsx a[data-tkxx]")
        .map { it.attr("data-tkxx") }
    LazyColumn(
        modifier = Modifier
            .padding(16.dp)
    ) {
        items(academicCourseChangeInfo) {
            Text("第一次")
            Text(it)
        }
    }
}